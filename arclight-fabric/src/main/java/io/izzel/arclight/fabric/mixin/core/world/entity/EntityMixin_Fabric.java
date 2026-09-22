package io.izzel.arclight.fabric.mixin.core.world.entity;

import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import io.izzel.arclight.common.bridge.core.world.damagesource.DamageSourceBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Bukkit;
import org.bukkit.entity.Hanging;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Entity.class)
public abstract class EntityMixin_Fabric implements EntityBridge {

    // @formatter:off
    @Shadow public abstract Level level();
    @Shadow public abstract boolean isRemoved();
    @Shadow private float yRot;
    @Shadow private float xRot;
    @Shadow public abstract float getXRot();
    @Shadow public abstract void absSnapTo(double x, double y, double z, float yRot, float xRot);
    @Shadow public abstract void setDeltaMovement(Vec3 vec3);
    @Shadow public abstract void unRide();
    @Shadow public abstract float getYRot();
    @Shadow public abstract EntityType<?> getType();
    @Shadow protected abstract void removeAfterChangingDimensions();
    @Shadow public abstract Vec3 position();
    @Shadow public abstract int getId();
    @Shadow public abstract void discard();
    @Shadow public abstract double getX();
    @Shadow public abstract double getY(double d);
    @Shadow public abstract double getZ();
    @Shadow public abstract boolean fireImmune();
    // @formatter:on

    @Decorate(method = "thunderHit", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private boolean arclight$onStruckByLightning$EntityCombustByEntityEvent1(Entity entity, ServerLevel serverLevel, DamageSource source, float amount, ServerLevel level, LightningBolt lightning) throws Throwable {
        final org.bukkit.entity.Entity thisBukkitEntity = this.bridge$getBukkitEntity();
        final org.bukkit.entity.Entity stormBukkitEntity = ((EntityBridge) lightning).bridge$getBukkitEntity();
        if (thisBukkitEntity instanceof Hanging hanging) {
            HangingBreakByEntityEvent hangingEvent = new HangingBreakByEntityEvent(hanging, stormBukkitEntity);
            Bukkit.getPluginManager().callEvent(hangingEvent);
            if (hangingEvent.isCancelled()) {
                return false;
            }
        }
        if (this.fireImmune()) {
            return false;
        }
        return (boolean) DecorationOps.callsite().invoke(entity, serverLevel, ((DamageSourceBridge) source).bridge$customCausingEntity(lightning), amount);
    }
}
