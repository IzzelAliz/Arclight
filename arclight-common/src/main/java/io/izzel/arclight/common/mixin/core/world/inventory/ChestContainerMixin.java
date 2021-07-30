package io.izzel.arclight.common.mixin.core.world.inventory;

import io.izzel.arclight.common.bridge.core.entity.player.PlayerEntityBridge;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
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

@Mixin(ChestMenu.class)
public abstract class ChestContainerMixin extends AbstractContainerMenuMixin {

    // @formatter:off
    @Shadow @Final private Container container;
    // @formatter:on

    private CraftInventoryView bukkitEntity;
    private Inventory playerInventory;

    @Inject(method = "<init>(Lnet/minecraft/world/inventory/MenuType;ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/Container;I)V", at = @At("RETURN"))
    public void arclight$init(MenuType<?> type, int id, Inventory playerInventoryIn, Container p_i50092_4_, int rows, CallbackInfo ci) {
        this.playerInventory = playerInventoryIn;
    }

    @Inject(method = "stillValid", cancellable = true, at = @At("HEAD"))
    public void arclight$unreachable(Player playerIn, CallbackInfoReturnable<Boolean> cir) {
        if (!bridge$isCheckReachable()) cir.setReturnValue(true);
    }

    @Override
    public CraftInventoryView getBukkitView() {
        if (bukkitEntity != null) {
            return bukkitEntity;
        }

        CraftInventory inventory;
        if (this.container instanceof Inventory) {
            inventory = new CraftInventoryPlayer((Inventory) this.container);
        } else if (this.container instanceof CompoundContainer) {
            inventory = new CraftInventoryDoubleChest((CompoundContainer) this.container);
        } else {
            inventory = new CraftInventory(this.container);
        }

        bukkitEntity = new CraftInventoryView(((PlayerEntityBridge) this.playerInventory.player).bridge$getBukkitEntity(), inventory, (AbstractContainerMenu) (Object) this);
        return bukkitEntity;
    }
}
