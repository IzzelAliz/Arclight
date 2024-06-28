package io.izzel.arclight.common.mixin.core.world.entity.boss.wither;

import io.izzel.arclight.common.mixin.core.world.entity.PathfinderMobMixin;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WitherBoss.class)
public abstract class WitherBossMixin extends PathfinderMobMixin {

    @Inject(method = "checkDespawn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/boss/wither/WitherBoss;discard()V"))
    private void arclight$despawn(CallbackInfo ci) {
        this.bridge$pushEntityRemoveCause(EntityRemoveEvent.Cause.DESPAWN);
    }

    @Decorate(method = "customServerAiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;explode(Lnet/minecraft/world/entity/Entity;DDDFZLnet/minecraft/world/level/Level$ExplosionInteraction;)Lnet/minecraft/world/level/Explosion;"))
    private Explosion arclight$explodeEvent(Level instance, Entity arg, double d, double e, double f, float radius, boolean fire, Level.ExplosionInteraction arg2) throws Throwable {
        ExplosionPrimeEvent event = new ExplosionPrimeEvent(this.getBukkitEntity(), radius, fire);
        Bukkit.getPluginManager().callEvent(event);
        if (!event.isCancelled()) {
            return (Explosion) DecorationOps.callsite().invoke(instance, arg, d, e, f, event.getRadius(), event.getFire(), arg2);
        }
        return null;
    }

    @Decorate(method = "customServerAiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/boss/wither/WitherBoss;setAlternativeTarget(II)V"))
    private void arclight$targetLivingEvent(WitherBoss instance, int i, int entityId) throws Throwable {
        if (i > 0 && entityId != 0) {
            if (CraftEventFactory.callEntityTargetLivingEvent((WitherBoss) (Object) this, (LivingEntity) this.level().getEntity(entityId), EntityTargetEvent.TargetReason.CLOSEST_ENTITY).isCancelled()) {
                return;
            }
        }
        DecorationOps.callsite().invoke(instance, i, entityId);
    }

    @Decorate(method = "customServerAiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;destroyBlock(Lnet/minecraft/core/BlockPos;ZLnet/minecraft/world/entity/Entity;)Z"))
    private boolean arclight$damageBlock(Level instance, BlockPos blockPos, boolean b, Entity entity) throws Throwable {
        if (!CraftEventFactory.callEntityChangeBlockEvent((WitherBoss) (Object) this, blockPos, Blocks.AIR.defaultBlockState())) {
            return false;
        }
        return (boolean) DecorationOps.callsite().invoke(instance, blockPos, b, entity);
    }

    @Inject(method = "customServerAiStep", at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/world/entity/boss/wither/WitherBoss;heal(F)V"))
    private void arclight$healReason(CallbackInfo ci) {
        bridge$pushHealReason(EntityRegainHealthEvent.RegainReason.WITHER_SPAWN);
    }

    @Inject(method = "customServerAiStep", at = @At(value = "INVOKE", ordinal = 1, target = "Lnet/minecraft/world/entity/boss/wither/WitherBoss;heal(F)V"))
    private void arclight$healReason2(CallbackInfo ci) {
        bridge$pushHealReason(EntityRegainHealthEvent.RegainReason.REGEN);
    }
}
