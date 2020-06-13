package io.izzel.arclight.common.mixin.v1_15.entity.passive;

import io.izzel.arclight.common.bridge.entity.LivingEntityBridge;
import io.izzel.arclight.common.mixin.core.entity.passive.AnimalEntityMixin;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.BeeEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.DamageSource;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BeeEntity.class)
public abstract class BeeEntityMixin extends AnimalEntityMixin {

    // @formatter:off
    @Shadow private BeeEntity.PollinateGoal pollinateGoal;
    @Shadow public abstract boolean setBeeAttacker(Entity attacker);
    // @formatter:on

    @Inject(method = "attackEntityAsMob", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;addPotionEffect(Lnet/minecraft/potion/EffectInstance;)Z"))
    private void arclight$sting(Entity entityIn, CallbackInfoReturnable<Boolean> cir) {
        ((LivingEntityBridge) entityIn).bridge$pushEffectCause(EntityPotionEffectEvent.Cause.ATTACK);
    }

    @Inject(method = "attackEntityAsMob", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/passive/BeeEntity;setAttackTarget(Lnet/minecraft/entity/LivingEntity;)V"))
    private void arclight$stungTarget(Entity entityIn, CallbackInfoReturnable<Boolean> cir) {
        bridge$pushGoalTargetReason(EntityTargetEvent.TargetReason.FORGOT_TARGET, true);
    }

    @Redirect(method = "attackEntityFrom", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/passive/AnimalEntity;attackEntityFrom(Lnet/minecraft/util/DamageSource;F)Z"))
    private boolean arclight$attackUpdateTarget(AnimalEntity animalEntity, DamageSource source, float amount) {
        boolean result = super.attackEntityFrom(source, amount);
        if (result && !this.world.isRemote && source.getTrueSource() instanceof PlayerEntity && !((PlayerEntity) source.getTrueSource()).isCreative() && this.canEntityBeSeen(source.getTrueSource()) && !this.isAIDisabled()) {
            this.pollinateGoal.cancel();
            this.setBeeAttacker(source.getTrueSource());
        }
        return result;
    }
}
