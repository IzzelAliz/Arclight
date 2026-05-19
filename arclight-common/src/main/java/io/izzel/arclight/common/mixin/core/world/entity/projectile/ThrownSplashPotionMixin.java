package io.izzel.arclight.common.mixin.core.world.entity.projectile;

import io.izzel.arclight.common.bridge.core.world.entity.LivingEntityBridge;
import io.izzel.arclight.common.bridge.core.world.level.WorldBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownSplashPotion;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import org.bukkit.craftbukkit.v.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mixin(ThrownSplashPotion.class)
public abstract class ThrownSplashPotionMixin extends ThrownPotionMixin {

    /**
     * @author IzzelAliz
     * @reason Bukkit PotionSplashEvent
     */
    @Overwrite
    public void method_67148(ServerLevel level, ItemStack stack, HitResult hitResult) {
        PotionContents potionContents = stack.getOrDefault(net.minecraft.core.component.DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        float scale = stack.getOrDefault(net.minecraft.core.component.DataComponents.POTION_DURATION_SCALE, 1.0F);
        Iterable<MobEffectInstance> effects = potionContents.getAllEffects();
        AABB hitBox = this.getBoundingBox().expandTowards(hitResult.getLocation().subtract(this.position())).inflate(4.0, 2.0, 4.0);
        AABB searchBox = hitBox.inflate(4.0, 2.0, 4.0);
        List<LivingEntity> entities = this.level().getEntitiesOfClass(LivingEntity.class, searchBox);
        Map<org.bukkit.entity.LivingEntity, Double> affected = new HashMap<>();
        if (!entities.isEmpty()) {
            for (LivingEntity entityLiving : entities) {
                if (!entityLiving.isAffectedByPotions()) {
                    continue;
                }
                double distance = hitBox.distanceToSqr(entityLiving.getBoundingBox().inflate(ProjectileUtil.computeMargin(entityLiving)));
                if (distance >= 16.0) {
                    continue;
                }
                double intensity = 1.0 - Math.sqrt(distance) / 4.0;
                affected.put(((LivingEntityBridge) entityLiving).bridge$getBukkitEntity(), intensity);
            }
        }
        PotionSplashEvent event = CraftEventFactory.callPotionSplashEvent((ThrownSplashPotion) (Object) this, hitResult, affected);
        if (!event.isCancelled()) {
            Entity owner = this.getOwner();
            for (org.bukkit.entity.LivingEntity victim : event.getAffectedEntities()) {
                if (!(victim instanceof CraftLivingEntity)) {
                    continue;
                }
                LivingEntity entityLiving = ((CraftLivingEntity) victim).getHandle();
                double intensity = event.getIntensity(victim);
                for (MobEffectInstance effect : effects) {
                    var holder = effect.getEffect();
                    if (!((WorldBridge) this.level()).bridge$isPvpMode() && owner instanceof ServerPlayer && entityLiving instanceof ServerPlayer && entityLiving != owner) {
                        var mobEffect = holder.value();
                        if (mobEffect == MobEffects.SLOWNESS || mobEffect == MobEffects.MINING_FATIGUE || mobEffect == MobEffects.INSTANT_DAMAGE || mobEffect == MobEffects.BLINDNESS
                            || mobEffect == MobEffects.HUNGER || mobEffect == MobEffects.WEAKNESS || mobEffect == MobEffects.POISON) {
                            continue;
                        }
                    }
                    if (holder.value().isInstantenous()) {
                        holder.value().applyInstantenousEffect(level, (ThrownSplashPotion) (Object) this, owner, entityLiving, effect.getAmplifier(), intensity);
                    } else {
                        int duration = effect.mapDuration(i -> (int) (intensity * (double) i * (double) scale + 0.5));
                        MobEffectInstance scaledEffect = new MobEffectInstance(holder, duration, effect.getAmplifier(), effect.isAmbient(), effect.isVisible());
                        if (scaledEffect.endsWithin(20)) {
                            continue;
                        }
                        ((LivingEntityBridge) entityLiving).bridge$pushEffectCause(EntityPotionEffectEvent.Cause.POTION_SPLASH);
                        entityLiving.addEffect(scaledEffect, owner);
                    }
                }
            }
        }
    }
}
