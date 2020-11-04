package io.izzel.arclight.common.mixin.core.inventory.container;

import io.izzel.arclight.common.bridge.entity.player.PlayerEntityBridge;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.container.BeaconContainer;
import net.minecraft.inventory.container.Container;
import net.minecraft.util.IIntArray;
import net.minecraft.util.IWorldPosCallable;
import org.bukkit.craftbukkit.v.inventory.CraftInventory;
import org.bukkit.craftbukkit.v.inventory.CraftInventoryBeacon;
import org.bukkit.craftbukkit.v.inventory.CraftInventoryView;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BeaconContainer.class)
public abstract class BeaconContainerMixin extends ContainerMixin {

    // @formatter:off
    @Shadow @Final private IInventory tileBeacon;
    // @formatter:on

    private CraftInventoryView bukkitEntity;
    private PlayerInventory playerInventory;

    @Inject(method = "<init>(ILnet/minecraft/inventory/IInventory;Lnet/minecraft/util/IIntArray;Lnet/minecraft/util/IWorldPosCallable;)V", at = @At("RETURN"))
    public void arclight$init(int id, IInventory inventory, IIntArray p_i50100_3_, IWorldPosCallable worldPosCallable, CallbackInfo ci) {
        this.playerInventory = (PlayerInventory) inventory;
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

        CraftInventory inventory = new CraftInventoryBeacon(this.tileBeacon);
        bukkitEntity = new CraftInventoryView(((PlayerEntityBridge) this.playerInventory.player).bridge$getBukkitEntity(), inventory, (Container) (Object) this);
        return bukkitEntity;
    }
}
