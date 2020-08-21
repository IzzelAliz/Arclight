package io.izzel.arclight.common.mixin.core.inventory.container;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.CraftResultInventory;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.container.AbstractRepairContainer;
import net.minecraft.util.IWorldPosCallable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractRepairContainer.class)
public abstract class AbstractRepairContainerMixin extends ContainerMixin {

    // @formatter:off
    @Shadow @Final protected IWorldPosCallable field_234644_e_;
    @Shadow @Final protected IInventory field_234643_d_;
    @Shadow @Final protected CraftResultInventory field_234642_c_;
    @Shadow @Final protected PlayerEntity field_234645_f_;
    // @formatter:on

    @Inject(method = "canInteractWith", cancellable = true, at = @At("HEAD"))
    private void arclight$unreachable(PlayerEntity playerIn, CallbackInfoReturnable<Boolean> cir) {
        if (!bridge$isCheckReachable()) cir.setReturnValue(true);
    }
}
