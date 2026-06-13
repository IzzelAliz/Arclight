package io.izzel.arclight.common.mixin.core.world.entity.projectile;

import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import io.izzel.arclight.common.bridge.core.world.entity.LivingEntityBridge;
import io.izzel.arclight.common.bridge.core.world.level.WorldBridge;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownSplashPotion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import org.bukkit.craftbukkit.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mixin(ThrownSplashPotion.class)
public abstract class ThrownSplashPotionMixin extends ThrowableItemProjectileMixin {

    @Unique private transient HitResult arclight$hitResult;

    @Inject(method = "onHitAsPotion", at = @At("HEAD"))
    private void arclight$captureHit(net.minecraft.server.level.ServerLevel level, net.minecraft.world.item.ItemStack stack, HitResult hitResult, CallbackInfo ci) {
        arclight$hitResult = hitResult;
    }

    @Inject(method = "onHit", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;discard()V"))
    private void arclight$hitCause(HitResult hitResult, CallbackInfo ci) {
        this.bridge$pushEntityRemoveCause(EntityRemoveEvent.Cause.HIT);
    }

    @Inject(method = "onHitAsPotion", at = @At("RETURN"))
    private void arclight$resetResult(net.minecraft.server.level.ServerLevel level, net.minecraft.world.item.ItemStack stack, HitResult hitResult, CallbackInfo ci) {
        arclight$hitResult = null;
    }

    /**
     * @author IzzelAliz
     * @reason Bukkit PotionSplashEvent
     */
    @Overwrite
    public void onHitAsPotion(net.minecraft.server.level.ServerLevel level, net.minecraft.world.item.ItemStack stack, HitResult hitResult) {
        PotionContents contents = stack.getOrDefault(net.minecraft.core.component.DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        float durationScale = stack.getOrDefault(net.minecraft.core.component.DataComponents.POTION_DURATION_SCALE, 1.0F);
        Iterable<MobEffectInstance> effects = contents.getAllEffects();
        AABB searchBox = this.getBoundingBox().move(hitResult.getLocation().subtract(this.position())).inflate(4.0, 2.0, 4.0);
        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, searchBox);
        Map<org.bukkit.entity.LivingEntity, Double> affected = new HashMap<>();
        if (!entities.isEmpty()) {
            Entity owner = this.getOwner();
            for (LivingEntity victim : entities) {
                if (!victim.isAffectedByPotions()) {
                    continue;
                }
                double distance = this.getBoundingBox().move(hitResult.getLocation().subtract(this.position())).distanceToSqr(victim.getBoundingBox().inflate(net.minecraft.world.entity.projectile.ProjectileUtil.computeMargin((net.minecraft.world.entity.Entity) (Object) this)));
                if (distance >= 16.0) {
                    continue;
                }
                double intensity = 1.0 - Math.sqrt(distance) / 4.0;
                if (victim == owner) {
                    intensity = 1.0;
                }
                affected.put(((LivingEntityBridge) victim).bridge$getBukkitEntity(), intensity);
            }
        }
        PotionSplashEvent event = CraftEventFactory.callPotionSplashEvent((ThrownSplashPotion) (Object) this, arclight$hitResult, affected);
        if (event.isCancelled()) {
            return;
        }
        for (org.bukkit.entity.LivingEntity victim : event.getAffectedEntities()) {
            if (!(victim instanceof CraftLivingEntity craftVictim)) {
                continue;
            }
            LivingEntity entity = craftVictim.getHandle();
            double intensity = event.getIntensity(victim);
            for (MobEffectInstance mobeffect : effects) {
                var holder = mobeffect.getEffect();
                if (!((WorldBridge) level).bridge$isPvpMode() && this.getOwner() instanceof ServerPlayer && entity instanceof ServerPlayer && entity != this.getOwner()) {
                    var mobeffectlist = holder.value();
                    if (holder.is(MobEffects.SLOWNESS) || holder.is(MobEffects.MINING_FATIGUE) || holder.is(MobEffects.INSTANT_DAMAGE) || holder.is(MobEffects.BLINDNESS)
                        || holder.is(MobEffects.HUNGER) || holder.is(MobEffects.WEAKNESS) || holder.is(MobEffects.POISON)) {
                        continue;
                    }
                }
                if (holder.value().isInstantenous()) {
                    holder.value().applyInstantenousEffect(level, (ThrownSplashPotion) (Object) this, this.getOwner(), entity, mobeffect.getAmplifier(), intensity);
                } else {
                    int duration = mobeffect.mapDuration(ticks -> (int) (intensity * ticks * durationScale + 0.5));
                    if (duration <= 20) {
                        continue;
                    }
                    ((LivingEntityBridge) entity).bridge$pushEffectCause(EntityPotionEffectEvent.Cause.POTION_SPLASH);
                    entity.addEffect(new MobEffectInstance(holder, duration, mobeffect.getAmplifier(), mobeffect.isAmbient(), mobeffect.isVisible()));
                }
            }
        }
    }
}
