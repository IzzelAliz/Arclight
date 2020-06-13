package io.izzel.arclight.common.mixin.core.inventory.container;

import io.izzel.arclight.common.bridge.entity.player.ServerPlayerEntityBridge;
import io.izzel.arclight.common.bridge.inventory.container.LecternContainerBridge;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.LecternContainer;
import net.minecraft.util.IIntArray;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.inventory.CraftInventoryLectern;
import org.bukkit.craftbukkit.v.inventory.CraftInventoryView;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTakeLecternBookEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LecternContainer.class)
public abstract class LecternContainerMixin extends ContainerMixin implements LecternContainerBridge {

    // @formatter:off
    @Shadow @Final private IInventory lecternInventory;
    // @formatter:on

    private CraftInventoryView bukkitEntity;
    private Player player;

    public void arclight$constructor(int i) {
        throw new RuntimeException();
    }

    public void arclight$constructor(int i, IInventory inventory, IIntArray intArray) {
        throw new RuntimeException();
    }

    public void arclight$constructor(int i, PlayerInventory playerInventory) {
        arclight$constructor(i);
        this.player = ((ServerPlayerEntityBridge) playerInventory.player).bridge$getBukkitEntity();
    }

    public void arclight$constructor(int i, IInventory inventory, IIntArray intArray, PlayerInventory playerInventory) {
        arclight$constructor(i, inventory, intArray);
        this.player = ((ServerPlayerEntityBridge) playerInventory.player).bridge$getBukkitEntity();
    }

    @Inject(method = "enchantItem", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/inventory/IInventory;removeStackFromSlot(I)Lnet/minecraft/item/ItemStack;"))
    public void arclight$takeBook(PlayerEntity playerIn, int id, CallbackInfoReturnable<Boolean> cir) {
        PlayerTakeLecternBookEvent event = new PlayerTakeLecternBookEvent(player, ((CraftInventoryLectern) getBukkitView().getTopInventory()).getHolder());
        Bukkit.getServer().getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "canInteractWith", cancellable = true, at = @At("HEAD"))
    public void arclight$unreachable(PlayerEntity playerIn, CallbackInfoReturnable<Boolean> cir) {
        if (!bridge$isCheckReachable()) cir.setReturnValue(true);
    }

    @Override
    public CraftInventoryView getBukkitView() {
        if (bukkitEntity != null) {
            return bukkitEntity;
        }
        CraftInventoryLectern inventory = new CraftInventoryLectern(this.lecternInventory);
        bukkitEntity = new CraftInventoryView(this.player, inventory, (Container) (Object) this);
        return bukkitEntity;
    }

    @Override
    public void bridge$setPlayerInventory(PlayerInventory playerInventory) {
        this.player = ((ServerPlayerEntityBridge) playerInventory.player).bridge$getBukkitEntity();
    }
}
