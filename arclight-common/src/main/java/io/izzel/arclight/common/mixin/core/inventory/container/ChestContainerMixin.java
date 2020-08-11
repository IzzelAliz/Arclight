package io.izzel.arclight.common.mixin.core.inventory.container;

import io.izzel.arclight.common.bridge.entity.player.PlayerEntityBridge;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.DoubleSidedInventory;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.container.ChestContainer;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.ContainerType;
import org.bukkit.craftbukkit.v.inventory.CraftInventory;
import org.bukkit.craftbukkit.v.inventory.CraftInventoryDoubleChest;
import org.bukkit.craftbukkit.v.inventory.CraftInventoryPlayer;
import org.bukkit.craftbukkit.v.inventory.CraftInventoryView;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChestContainer.class)
public abstract class ChestContainerMixin extends ContainerMixin {

    // @formatter:off
    @Shadow @Final private IInventory lowerChestInventory;
    // @formatter:on

    private CraftInventoryView bukkitEntity;
    private PlayerInventory playerInventory;

    @Inject(method = "<init>(Lnet/minecraft/inventory/container/ContainerType;ILnet/minecraft/entity/player/PlayerInventory;Lnet/minecraft/inventory/IInventory;I)V", at = @At("RETURN"))
    public void arclight$init(ContainerType<?> type, int id, PlayerInventory playerInventoryIn, IInventory p_i50092_4_, int rows, CallbackInfo ci) {
        this.playerInventory = playerInventoryIn;
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

        CraftInventory inventory;
        if (this.lowerChestInventory instanceof PlayerInventory) {
            inventory = new CraftInventoryPlayer((PlayerInventory) this.lowerChestInventory);
        } else if (this.lowerChestInventory instanceof DoubleSidedInventory) {
            inventory = new CraftInventoryDoubleChest((DoubleSidedInventory) this.lowerChestInventory);
        } else {
            inventory = new CraftInventory(this.lowerChestInventory);
        }

        bukkitEntity = new CraftInventoryView(((PlayerEntityBridge) this.playerInventory.player).bridge$getBukkitEntity(), inventory, (Container) (Object) this);
        return bukkitEntity;
    }
}
