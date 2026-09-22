package io.izzel.arclight.fabric.mixin.core.world.level;

import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ServerExplosion;
import net.minecraft.world.phys.Vec3;
import org.bukkit.craftbukkit.v.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.entity.EntityKnockbackEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(ServerExplosion.class)
public abstract class ExplosionMixin_Fabric {

    @Shadow @Final private Entity source;

    @Decorate(method = "hurtEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private boolean arclight$handleMultiPart(Entity entity, ServerLevel level, DamageSource damageSource, float damage,
                                             @Local(ordinal = -1) List<Entity> entities) throws Throwable {
        // Special case ender dragon: only give knockback if the damage was not cancelled.
        if (((EntityBridge) entity).bridge$forge$isPartEntity()) {
            throw DecorationOps.jumpToLoopStart();
        }

        ((EntityBridge) entity).bridge$setLastDamageCancelled(false);
        boolean result = false;
        var parts = ((EntityBridge) entity).bridge$forge$getParts();
        if (parts != null) {
            for (var part : parts) {
                if (entities.contains(part)) {
                    part.hurtServer(level, damageSource, damage);
                    result = true;
                }
            }
        } else {
            result = (boolean) DecorationOps.callsite().invoke(entity, level, damageSource, damage);
        }

        if (((EntityBridge) entity).bridge$isLastDamageCancelled()) {
            throw DecorationOps.jumpToLoopStart();
        }
        return result;
    }

    @Decorate(method = "hurtEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/Vec3;scale(D)Lnet/minecraft/world/phys/Vec3;"))
    private Vec3 arclight$knockBack(Vec3 direction, double scale, @Local(ordinal = -1) Entity entity) throws Throwable {
        var knockback = (Vec3) DecorationOps.callsite().invoke(direction, scale);
        if (entity instanceof LivingEntity) {
            var result = entity.getDeltaMovement().add(knockback);
            var event = CraftEventFactory.callEntityKnockbackEvent((CraftLivingEntity) ((EntityBridge) entity).bridge$getBukkitEntity(), this.source,
                EntityKnockbackEvent.KnockbackCause.EXPLOSION, knockback.length(), knockback,
                result.x, result.y, result.z);
            knockback = event.isCancelled() ? Vec3.ZERO : new Vec3(event.getFinalKnockback().getX(), event.getFinalKnockback().getY(), event.getFinalKnockback().getZ()).subtract(entity.getDeltaMovement());
        }
        return knockback;
    }
}
