package io.izzel.arclight.common.mixin.core.world.entity.projectile;

import com.destroystokyo.paper.event.entity.EnderDragonFireballHitEvent;
import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import io.izzel.arclight.common.bridge.core.world.level.WorldBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.hurtingprojectile.DragonFireball;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(DragonFireball.class)
public abstract class DragonFireballMixin extends ProjectileMixin {

    // The fourth level() read starts the splash tail, after native cloud positioning.
    @Decorate(method = "onHit(Lnet/minecraft/world/phys/HitResult;)V", inject = true,
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/hurtingprojectile/DragonFireball;level()Lnet/minecraft/world/level/Level;", ordinal = 3), require = 1)
    private void arclight$fireballHitEvent(@Local(ordinal = 0) List<LivingEntity> entities,
                                         @Local(ordinal = 0) AreaEffectCloud cloud,
                                         @Local(allocate = "arclightDragonHitCancelled") boolean cancelled) throws Throwable {
        var targets = new ArrayList<org.bukkit.entity.LivingEntity>(entities.size());
        for (LivingEntity entity : entities) {
            targets.add((org.bukkit.entity.LivingEntity) entity.bridge$getBukkitEntity());
        }
        var event = new EnderDragonFireballHitEvent((org.bukkit.entity.DragonFireball) this.bridge$getBukkitEntity(),
            targets, (org.bukkit.entity.AreaEffectCloud) cloud.bridge$getBukkitEntity());
        cancelled = !event.callEvent();
        DecorationOps.blackhole().invoke(cancelled);
        if (cancelled) {
            ((EntityBridge) cloud).bridge$pushEntityRemoveCause(null);
            cloud.discard();
        }
    }

    @Decorate(method = "onHit(Lnet/minecraft/world/phys/HitResult;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;levelEvent(ILnet/minecraft/core/BlockPos;I)V"), require = 1)
    private void arclight$fireballSplash(Level level, int event, BlockPos pos, int data,
                                       @Local(allocate = "arclightDragonHitCancelled") boolean cancelled) throws Throwable {
        if (!cancelled) {
            DecorationOps.callsite().invoke(level, event, pos, data);
        }
    }

    @Decorate(method = "onHit(Lnet/minecraft/world/phys/HitResult;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"), require = 1)
    private boolean arclight$fireballCloud(Level level, Entity cloud,
                                         @Local(allocate = "arclightDragonHitCancelled") boolean cancelled) throws Throwable {
        if (cancelled) {
            return false;
        }
        var world = (WorldBridge) level;
        var previousReason = world.bridge$getAddEntityReason();
        world.bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.EXPLOSION);
        try {
            return (boolean) DecorationOps.callsite().invoke(level, cloud);
        } finally {
            world.bridge$pushAddEntityReason(previousReason);
        }
    }

    @Inject(method = "onHit", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/hurtingprojectile/DragonFireball;discard()V"))
    private void arclight$hit(HitResult hitResult, CallbackInfo ci) {
        this.bridge$pushEntityRemoveCause(EntityRemoveEvent.Cause.HIT);
    }
}
