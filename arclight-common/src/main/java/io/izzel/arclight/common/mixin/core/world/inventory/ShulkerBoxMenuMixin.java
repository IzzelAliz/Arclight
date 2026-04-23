package io.izzel.arclight.common.mixin.core.world.inventory;

import io.izzel.arclight.common.bridge.core.world.entity.player.PlayerBridge;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import org.bukkit.craftbukkit.v.inventory.CraftInventory;
import org.bukkit.craftbukkit.v.inventory.CraftInventoryView;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ShulkerBoxMenu.class)
public abstract class ShulkerBoxMenuMixin extends AbstractContainerMenuMixin {

    // @formatter:off
    @Shadow @Final private Container container;
    // @formatter:on

    private CraftInventoryView<ShulkerBoxMenu, ?> bukkitEntity;
    private Inventory playerInventory;

    @Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/Container;)V", at = @At("RETURN"))
    public void arclight$init(int id, Inventory playerInventory, Container inventory, CallbackInfo ci) {
        this.playerInventory = playerInventory;
    }

    @Inject(method = "stillValid", cancellable = true, at = @At("HEAD"))
    public void arclight$unreachable(net.minecraft.world.entity.player.Player playerIn, CallbackInfoReturnable<Boolean> cir) {
        if (!bridge$isCheckReachable()) cir.setReturnValue(true);
    }

    @Override
    public CraftInventoryView<ShulkerBoxMenu, ?> getBukkitView() {
        if (bukkitEntity != null) {
            return bukkitEntity;
        }

        bukkitEntity = new CraftInventoryView<>(((PlayerBridge) this.playerInventory.player).bridge$getBukkitEntity(), new CraftInventory(this.container), (ShulkerBoxMenu) (Object) this);
        return bukkitEntity;
    }
}
