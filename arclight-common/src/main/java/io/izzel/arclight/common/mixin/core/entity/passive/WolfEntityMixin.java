package io.izzel.arclight.common.mixin.core.entity.passive;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.SitGoal;
import net.minecraft.entity.passive.WolfEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Hand;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WolfEntity.class)
public abstract class WolfEntityMixin extends TameableEntityMixin {

    // @formatter:off
    @Shadow public abstract void setAngry(boolean angry);
    @Shadow public abstract boolean isAngry();
    // @formatter:on

    @Override
    public boolean setGoalTarget(LivingEntity entityliving, EntityTargetEvent.TargetReason reason, boolean fire) {
        if (!super.setGoalTarget(entityliving, reason, fire)) {
            return false;
        }
        entityliving = getAttackingEntity();
        if (entityliving == null) {
            this.setAngry(false);
        } else if (!this.isTamed()) {
            this.setAngry(true);
        }
        return true;
    }

    @Inject(method = "readAdditional", at = @At(value = "INVOKE", target = "Lnet/minecraft/nbt/CompoundNBT;contains(Ljava/lang/String;I)Z"))
    private void arclight$angry(CompoundNBT compound, CallbackInfo ci) {
        if (this.getAttackTarget() == null && this.isAngry()) {
            this.setAngry(false);
        }
    }

    @Redirect(method = "livingTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/passive/WolfEntity;setAngry(Z)V"))
    private void arclight$fixError(WolfEntity wolfEntity, boolean angry) {
    }

    @Redirect(method = "attackEntityFrom", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/ai/goal/SitGoal;setSitting(Z)V"))
    private void arclight$handledBy(SitGoal sitGoal, boolean sitting) {
    }

    @Inject(method = "setTamed", at = @At("RETURN"))
    private void arclight$healToMax(boolean tamed, CallbackInfo ci) {
        if (tamed) {
            this.setHealth(this.getMaxHealth());
        }
    }

    @Inject(method = "processInteract", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/passive/WolfEntity;heal(F)V"))
    private void arclight$healReason(PlayerEntity player, Hand hand, CallbackInfoReturnable<Boolean> cir) {
        bridge$pushHealReason(EntityRegainHealthEvent.RegainReason.EATING);
    }

    @Inject(method = "processInteract", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/passive/WolfEntity;setAttackTarget(Lnet/minecraft/entity/LivingEntity;)V"))
    private void arclight$attackReason(PlayerEntity player, Hand hand, CallbackInfoReturnable<Boolean> cir) {
        bridge$pushGoalTargetReason(EntityTargetEvent.TargetReason.FORGOT_TARGET, true);
    }
}
