package io.izzel.arclight.common.mixin.vanilla.world.entity;

import io.izzel.arclight.common.bridge.core.entity.LivingEntityBridge;
import io.izzel.arclight.common.bridge.vanilla.world.entity.LivingEntityBridge_Vanilla;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import io.izzel.arclight.common.mod.util.ArclightDamageContainer;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin_Vanilla extends EntityMixin_Vanilla implements LivingEntityBridge, LivingEntityBridge_Vanilla {

    @Inject(method = "dropAllDeathLoot", at = @At("HEAD"))
    private void arclight$startCapture(ServerLevel serverLevel, DamageSource damageSource, CallbackInfo ci) {
        this.arclight$startCaptureDrops();
    }

    @Inject(method = "dropAllDeathLoot", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;dropExperience(Lnet/minecraft/world/entity/Entity;)V"))
    private void arclight$stopCapture(ServerLevel serverLevel, DamageSource damageSource, CallbackInfo ci) {
        final var list = this.arclight$finishCaptureDrops();
        this.arclight$vanilla$callLivingDropsEvent(damageSource, list);
        list.forEach(serverLevel::addFreshEntity);
    }

    @Decorate(method = "hurt", inject = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;isSleeping()Z"))
    private void arclight$entityDamageEvent(DamageSource damagesource, float originalDamage, @Local(allocate = "arclightDamageContainer") ArclightDamageContainer container) throws Throwable {
        EntityDamageEvent event = arclight$fireEntityDamageEvent(damagesource, originalDamage);

        if (event == null || event.isCancelled()) {
            DecorationOps.cancel().invoke(false);
            return;
        }

        container = new ArclightDamageContainer(event);
        originalDamage = (float) event.getDamage();
        DecorationOps.blackhole().invoke(container, originalDamage);

        if (damagesource.getEntity() instanceof net.minecraft.world.entity.player.Player) {
            ((net.minecraft.world.entity.player.Player) damagesource.getEntity()).resetAttackStrengthTicker();
        }
    }

    // Special handle; assuming shield cut out at most BLOCKING damage from original and g means the damage that will be used to calculate exact blocking.
    // Merged Bukkit & Modded calculation result with the above assumption. Be careful.
    /*
    @Decorate(method = "hurt", inject = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;isDamageSourceBlocked(Lnet/minecraft/world/damagesource/DamageSource;)Z"))
    private void arclight$vanilla$preApplyShield(DamageSource source, float original, @Local(allocate = "arclightDamageContainer") ArclightDamageContainer container) throws Throwable {
        double blocking = event.getDamage(EntityDamageEvent.DamageModifier.BLOCKING);
        if (blocking == 0.0F) return;
        original = -(float) blocking;
        DecorationOps.blackhole().invoke(original);
    }
    @Decorate(method = "hurt", inject = true, at = @At(value = "INVOKE", ordinal = 2, target = "Lnet/minecraft/world/damagesource/DamageSource;is(Lnet/minecraft/tags/TagKey;)Z"))
    private void arclight$vanilla$postApplyShield(DamageSource source, float original, @Local(allocate = "arclightDamageContainer") ArclightDamageContainer container) throws Throwable {
        double before = -event.getDamage(EntityDamageEvent.DamageModifier.BLOCKING);
        if (before == -0.0F) return;
        double actualOffset = before - original;
        event.setDamage(EntityDamageEvent.DamageModifier.BLOCKING, actualOffset);
    }
    */

    @Decorate(method = "hurt", inject = true, at = @At(value = "INVOKE", ordinal = 2, target = "Lnet/minecraft/world/damagesource/DamageSource;is(Lnet/minecraft/tags/TagKey;)Z"))
    private void arclight$vanilla$postApplyShield(DamageSource source, float original, @Local(allocate = "arclightDamageContainer") ArclightDamageContainer container) throws Throwable {
        original = container.calculateStage(EntityDamageEvent.DamageModifier.BLOCKING, original);
        DecorationOps.blackhole().invoke(original);
    }

    @Decorate(method = "hurt", inject = true, at = @At(value = "INVOKE", ordinal = 3, target = "Lnet/minecraft/world/damagesource/DamageSource;is(Lnet/minecraft/tags/TagKey;)Z"))
    private void arclight$vanilla$postApplyFreezing(DamageSource source, float original, @Local(allocate = "arclightDamageContainer") ArclightDamageContainer container) throws Throwable {
        original = container.calculateStage(EntityDamageEvent.DamageModifier.FREEZING, original);
        DecorationOps.blackhole().invoke(original);
    }

    @Decorate(method = "hurt", inject = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/WalkAnimationState;setSpeed(F)V"))
    private void arclight$vanilla$postApplyHardHat(DamageSource source, float original, @Local(allocate = "arclightDamageContainer") ArclightDamageContainer container) throws Throwable {
        original = container.calculateStage(EntityDamageEvent.DamageModifier.HARD_HAT, original);
        DecorationOps.blackhole().invoke(original);
    }

    @Decorate(method = "hurt", inject = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;actuallyHurt(Lnet/minecraft/world/damagesource/DamageSource;F)V"))
    private void arclight$vanilla$captureEntityDamageEvent(DamageSource source, float original, @Local(allocate = "arclightDamageContainer") ArclightDamageContainer container) throws Throwable {
        ArclightCaptures.captureDamageContainer(container);
    }

    @Decorate(method = "actuallyHurt", inject = true, at = @At("HEAD"))
    private void arclight$vanilla$getEntityDamageEvent(DamageSource damageSource, float f, @Local(allocate = "arclightDamageContainer") ArclightDamageContainer container) throws Throwable {
        container = ArclightCaptures.getDamageContainer();
        DecorationOps.blackhole().invoke(container);
    }

    @Decorate(method = "actuallyHurt", inject = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getDamageAfterMagicAbsorb(Lnet/minecraft/world/damagesource/DamageSource;F)F"))
    private void arclight$vanilla$postApplyArmor(DamageSource source, float original, @Local(allocate = "arclightDamageContainer") ArclightDamageContainer container) throws Throwable {
        original = container.calculateStage(EntityDamageEvent.DamageModifier.ARMOR, original);
        DecorationOps.blackhole().invoke(original);
    }

    @Decorate(method = "getDamageAfterMagicAbsorb", at = @At(value = "INVOKE", target = "Ljava/lang/Math;max(FF)F"))
    private float arclight$vanilla$postApplyResistance(float first, float second) throws Throwable {
        float result = (float) DecorationOps.callsite().invoke(first, second);
        result = ArclightCaptures.getDamageContainer().calculateStage(EntityDamageEvent.DamageModifier.RESISTANCE, result);
        return result;
    }

    @Decorate(method = "actuallyHurt", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getDamageAfterMagicAbsorb(Lnet/minecraft/world/damagesource/DamageSource;F)F"))
    private float arclight$vanilla$postApplyMagic(LivingEntity entity, DamageSource source, float original, @Local(allocate = "arclightDamageContainer") ArclightDamageContainer container) throws Throwable {
        float result = (float) DecorationOps.callsite().invoke(entity, source, original);
        return container.calculateStage(EntityDamageEvent.DamageModifier.MAGIC, result);
    }

    @Decorate(method = "actuallyHurt", at = @At(value = "INVOKE", target = "Ljava/lang/Math;max(FF)F"))
    private float arclight$vanilla$postApplyAbsorption(float first, float second, @Local(allocate = "arclightDamageContainer") ArclightDamageContainer container) throws Throwable {
        float result = (float) DecorationOps.callsite().invoke(first, second);
        result = container.calculateStage(EntityDamageEvent.DamageModifier.ABSORPTION, result);
        return Math.max(result, 0.0F);
    }

    @Inject(method = "actuallyHurt", at = @At("RETURN"))
    private void arclight$vanilla$popEntityDamageEvent(DamageSource arg, float g, CallbackInfo ci) {
        ArclightCaptures.popDamageContainer();
    }
}
