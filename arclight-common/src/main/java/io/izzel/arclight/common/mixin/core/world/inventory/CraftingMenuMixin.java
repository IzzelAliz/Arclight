package io.izzel.arclight.common.mixin.core.world.inventory;

import io.izzel.arclight.common.bridge.core.entity.player.PlayerEntityBridge;
import io.izzel.arclight.common.bridge.core.inventory.CraftingInventoryBridge;
import io.izzel.arclight.common.bridge.core.inventory.container.ContainerBridge;
import io.izzel.arclight.common.bridge.core.inventory.container.PosContainerBridge;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RepairItemRecipe;
import net.minecraft.world.level.Level;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.craftbukkit.v.inventory.CraftInventoryCrafting;
import org.bukkit.craftbukkit.v.inventory.CraftInventoryView;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(CraftingMenu.class)
public abstract class CraftingMenuMixin extends AbstractContainerMenuMixin implements PosContainerBridge {

    // @formatter:off
    @Mutable @Shadow @Final private CraftingContainer craftSlots;
    @Shadow @Final private ResultContainer resultSlots;
    @Accessor("access") public abstract ContainerLevelAccess bridge$getWorldPos();
    // @formatter:on

    private CraftInventoryView bukkitEntity;
    private Inventory playerInventory;

    @Inject(method = "stillValid", cancellable = true, at = @At("HEAD"))
    public void arclight$unreachable(Player playerIn, CallbackInfoReturnable<Boolean> cir) {
        if (!bridge$isCheckReachable()) cir.setReturnValue(true);
    }

    @Inject(method = "slotsChanged", at = @At("HEAD"))
    public void arclight$capture(Container inventoryIn, CallbackInfo ci) {
        ArclightCaptures.captureWorkbenchContainer((CraftingMenu) (Object) this);
    }

    private static transient boolean arclight$isRepair;

    @Redirect(method = "slotChangedCraftingGrid", at = @At(value = "INVOKE", remap = false, target = "Ljava/util/Optional;isPresent()Z"))
    private static boolean arclight$testRepair(Optional<?> optional) {
        arclight$isRepair = optional.orElse(null) instanceof RepairItemRecipe;
        return optional.isPresent();
    }

    @ModifyVariable(method = "slotChangedCraftingGrid", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/inventory/ResultContainer;setItem(ILnet/minecraft/world/item/ItemStack;)V"))
    private static ItemStack arclight$preCraft(ItemStack stack, AbstractContainerMenu container, Level level, Player player, CraftingContainer craftingContainer, ResultContainer resultContainer) {
        return CraftEventFactory.callPreCraftEvent(craftingContainer, resultContainer, stack, ((ContainerBridge) container).bridge$getBukkitView(), arclight$isRepair);
    }

    @Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V", at = @At("RETURN"))
    public void arclight$init(int i, Inventory playerInventory, ContainerLevelAccess callable, CallbackInfo ci) {
        ((CraftingInventoryBridge) this.craftSlots).bridge$setOwner(playerInventory.player);
        ((CraftingInventoryBridge) this.craftSlots).bridge$setResultInventory(this.resultSlots);
        this.playerInventory = playerInventory;
    }

    @Override
    public CraftInventoryView getBukkitView() {
        if (bukkitEntity != null) {
            return bukkitEntity;
        }

        CraftInventoryCrafting inventory = new CraftInventoryCrafting(this.craftSlots, this.resultSlots);
        bukkitEntity = new CraftInventoryView(((PlayerEntityBridge) this.playerInventory.player).bridge$getBukkitEntity(), inventory, (AbstractContainerMenu) (Object) this);
        return bukkitEntity;
    }
}
