package io.izzel.arclight.common.mixin.core.entity.passive;

import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WolfEntity.class)
public abstract class WolfEntityMixin extends TameableEntityMixin {

    @Redirect(method = "attackEntityFrom", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/passive/WolfEntity;func_233687_w_(Z)V"))
    private void arclight$handledBy(WolfEntity wolfEntity, boolean p_233687_1_) {
    }

    @Inject(method = "setTamed", at = @At("RETURN"))
    private void arclight$healToMax(boolean tamed, CallbackInfo ci) {
        if (tamed) {
            this.setHealth(this.getMaxHealth());
        }
    }

    @Inject(method = "func_230254_b_", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/passive/WolfEntity;heal(F)V"))
    private void arclight$healReason(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResultType> cir) {
        bridge$pushHealReason(EntityRegainHealthEvent.RegainReason.EATING);
    }

    @Inject(method = "func_230254_b_", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/passive/WolfEntity;setAttackTarget(Lnet/minecraft/entity/LivingEntity;)V"))
    private void arclight$attackReason(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResultType> cir) {
        bridge$pushGoalTargetReason(EntityTargetEvent.TargetReason.FORGOT_TARGET, true);
    }
}
