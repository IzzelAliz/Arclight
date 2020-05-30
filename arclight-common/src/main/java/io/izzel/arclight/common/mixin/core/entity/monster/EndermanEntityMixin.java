package io.izzel.arclight.common.mixin.core.entity.monster;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.monster.EndermanEntity;
import org.bukkit.event.entity.EntityTargetEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import io.izzel.arclight.common.mixin.core.entity.CreatureEntityMixin;

import javax.annotation.Nullable;

@Mixin(EndermanEntity.class)
public abstract class EndermanEntityMixin extends CreatureEntityMixin {

    // @formatter:off
    @Shadow public abstract void setAttackTarget(@Nullable LivingEntity entitylivingbaseIn);
    // @formatter:on

    @Override
    public boolean setGoalTarget(LivingEntity livingEntity, EntityTargetEvent.TargetReason reason, boolean fireEvent) {
        if (!super.setGoalTarget(livingEntity, reason, fireEvent)) {
            return false;
        } else {
            setAttackTarget(getAttackTarget());
        }
        return true;
    }

    @Inject(method = "setAttackTarget", cancellable = true, at = @At(value = "INVOKE", remap = false, target = "Lnet/minecraft/entity/monster/MonsterEntity;setAttackTarget(Lnet/minecraft/entity/LivingEntity;)V"))
    private void arclight$muteSuper(LivingEntity entitylivingbaseIn, CallbackInfo ci) {
        ci.cancel();
    }
}
