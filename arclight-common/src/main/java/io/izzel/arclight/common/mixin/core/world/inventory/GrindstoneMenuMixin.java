package io.izzel.arclight.common.mixin.core.world.inventory;

import io.izzel.arclight.common.bridge.core.world.entity.player.PlayerBridge;
import io.izzel.arclight.common.bridge.core.world.inventory.PosContainerBridge;
import io.izzel.arclight.common.mod.server.world.inventory.ArclightGrindstoneView;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.item.ItemStack;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.craftbukkit.v.inventory.CraftInventoryGrindstone;
import org.bukkit.craftbukkit.v.inventory.CraftInventoryView;
import org.bukkit.craftbukkit.v.inventory.CraftItemStack;
import org.bukkit.event.inventory.PrepareGrindstoneEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GrindstoneMenu.class)
public abstract class GrindstoneMenuMixin extends AbstractContainerMenuMixin implements PosContainerBridge {

    @Shadow @Final Container repairSlots;
    @Shadow @Final private Container resultSlots;
    @Shadow @Final private ContainerLevelAccess access;
    private CraftInventoryView<GrindstoneMenu, ?> bukkitEntity = null;
    private Inventory playerInventory;

    @Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V", at = @At("RETURN"))
    public void arclight$init(int windowIdIn, Inventory playerInventory, ContainerLevelAccess worldPosCallableIn, CallbackInfo ci) {
        this.playerInventory = playerInventory;
    }

    @Decorate(method = "createResult", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/Container;setItem(ILnet/minecraft/world/item/ItemStack;)V"))
    private void arclight$prepareEvent(Container instance, int i, ItemStack itemStack) throws Throwable {
        final CraftInventoryView<GrindstoneMenu, ?> craft = getBukkitView();
        if (craft instanceof ArclightGrindstoneView) {
            // Call prepare event; preserve injection point
            PrepareGrindstoneEvent event = new PrepareGrindstoneEvent(craft, CraftItemStack.asCraftMirror(itemStack).clone());
            event.getView().getPlayer().getServer().getPluginManager().callEvent(event);
            DecorationOps.callsite().invoke(instance, 2, CraftItemStack.asNMSCopy(event.getResult()));
        } else {
            // Run plugin custom logic
            CraftEventFactory.callPrepareGrindstoneEvent(getBukkitView(), itemStack);
        }
    }

    @Inject(method = "createResult", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/GrindstoneMenu;broadcastChanges()V"))
    private void arclight$sync(CallbackInfo ci) {
        sendAllDataToRemote();
    }

    @Override
    public CraftInventoryView<GrindstoneMenu, ?> getBukkitView() {
        if (bukkitEntity != null) {
            return bukkitEntity;
        }

        CraftInventoryGrindstone inventory = new CraftInventoryGrindstone(this.repairSlots, this.resultSlots);
        bukkitEntity = new ArclightGrindstoneView(((PlayerBridge) this.playerInventory.player).bridge$getBukkitEntity(), inventory, (GrindstoneMenu) (Object) this);
        return bukkitEntity;
    }

    @Override
    public ContainerLevelAccess bridge$getWorldPos() {
        return this.access;
    }
}
