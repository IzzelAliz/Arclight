package io.izzel.arclight.common.mixin.core.world.inventory;

import io.izzel.arclight.common.bridge.core.world.entity.player.PlayerBridge;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.BrewingStandMenu;
import net.minecraft.world.inventory.ContainerData;
import org.bukkit.craftbukkit.inventory.CraftInventoryBrewer;
import org.bukkit.craftbukkit.inventory.CraftInventoryView;
import org.bukkit.craftbukkit.inventory.view.CraftBrewingStandView;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BrewingStandMenu.class)
public abstract class BrewingStandMenuMixin extends AbstractContainerMenuMixin {

    // @formatter:off
    @Shadow @Final private Container brewingStand;
    // @formatter:on

    private CraftBrewingStandView view = null; // Paper added
    private Inventory inventory; // Paper added

    @Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/Container;Lnet/minecraft/world/inventory/ContainerData;)V", at = @At("RETURN"))
    public void arclight$init(int id, Inventory playerInventory, Container inventory, ContainerData p_i50096_4_, CallbackInfo ci) {
        this.inventory = playerInventory;
    }

    @Inject(method = "stillValid", cancellable = true, at = @At("HEAD"))
    public void arclight$unreachable(Player playerIn, CallbackInfoReturnable<Boolean> cir) {
        if (!bridge$isCheckReachable()) cir.setReturnValue(true);
    }

    @Override
    public CraftBrewingStandView getBukkitView() {
        if (this.view != null) {
            return this.view;
        }

        CraftInventoryBrewer inventory = new CraftInventoryBrewer(this.brewingStand);
        this.view = new CraftBrewingStandView(((PlayerBridge) this.inventory.player).bridge$getBukkitEntity(), inventory, (BrewingStandMenu) (Object) this);
        return this.view;
    }
}
