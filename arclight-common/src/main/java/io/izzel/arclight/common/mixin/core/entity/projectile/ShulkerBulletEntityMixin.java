package io.izzel.arclight.common.mixin.core.entity.projectile;

import io.izzel.arclight.common.bridge.entity.LivingEntityBridge;
import io.izzel.arclight.common.mixin.core.entity.EntityMixin;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ShulkerBulletEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

@Mixin(ShulkerBulletEntity.class)
public abstract class ShulkerBulletEntityMixin extends EntityMixin {

    // @formatter:off
    @Shadow private LivingEntity owner;
    @Shadow private Entity target;
    @Shadow @Nullable private Direction direction;
    @Shadow protected abstract void selectNextMoveDirection(@Nullable Direction.Axis p_184569_1_);
    // @formatter:on

    @Inject(method = "<init>(Lnet/minecraft/world/World;Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/entity/Entity;Lnet/minecraft/util/Direction$Axis;)V", at = @At("RETURN"))
    private void arclight$init(World worldIn, LivingEntity ownerIn, Entity targetIn, Direction.Axis p_i46772_4_, CallbackInfo ci) {
        this.projectileSource = ((LivingEntityBridge) ownerIn).bridge$getBukkitEntity();
    }

    @Inject(method = "bulletHit", at = @At("HEAD"))
    private void arclight$projectileHit(RayTraceResult result, CallbackInfo ci) {
        CraftEventFactory.callProjectileHitEvent((ShulkerBulletEntity) (Object) this, result);
    }

    @Inject(method = "bulletHit", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;addPotionEffect(Lnet/minecraft/potion/EffectInstance;)Z"))
    private void arclight$reason(RayTraceResult result, CallbackInfo ci) {
        ((LivingEntityBridge) ((EntityRayTraceResult) result).getEntity()).bridge$pushEffectCause(EntityPotionEffectEvent.Cause.ATTACK);
    }

    public LivingEntity getShooter() {
        return this.owner;
    }

    public void setShooter(final LivingEntity e) {
        this.owner = e;
    }

    public Entity getTarget() {
        return this.target;
    }

    public void setTarget(final Entity e) {
        this.target = e;
        this.direction = Direction.UP;
        this.selectNextMoveDirection(Direction.Axis.X);
    }
}
