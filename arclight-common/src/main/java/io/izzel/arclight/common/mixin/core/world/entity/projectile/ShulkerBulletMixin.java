package io.izzel.arclight.common.mixin.core.world.entity.projectile;

import io.izzel.arclight.common.bridge.core.entity.LivingEntityBridge;
import io.izzel.arclight.common.mixin.core.world.entity.EntityMixin;
import net.minecraft.core.Direction;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

@Mixin(ShulkerBullet.class)
public abstract class ShulkerBulletMixin extends EntityMixin {

    // @formatter:off
    @Shadow private Entity finalTarget;
    @Shadow @Nullable private Direction currentMoveDirection;
    @Shadow protected abstract void selectNextMoveDirection(@Nullable Direction.Axis p_184569_1_);
    // @formatter:on

    @Inject(method = "<init>(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/core/Direction$Axis;)V", at = @At("RETURN"))
    private void arclight$init(Level worldIn, LivingEntity ownerIn, Entity targetIn, Direction.Axis p_i46772_4_, CallbackInfo ci) {
        this.projectileSource = ((LivingEntityBridge) ownerIn).bridge$getBukkitEntity();
    }

    @Inject(method = "onHitEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z"))
    private void arclight$reason(EntityHitResult result, CallbackInfo ci) {
        ((LivingEntityBridge) result.getEntity()).bridge$pushEffectCause(EntityPotionEffectEvent.Cause.ATTACK);
    }

    @Inject(method = "hurt", cancellable = true, at = @At("HEAD"))
    private void arclight$damageBullet(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (CraftEventFactory.handleNonLivingEntityDamageEvent((ShulkerBullet) (Object) this, source, amount, false)) {
            cir.setReturnValue(false);
        }
    }

    public Entity getTarget() {
        return this.finalTarget;
    }

    public void setTarget(final Entity e) {
        this.finalTarget = e;
        this.currentMoveDirection = Direction.UP;
        this.selectNextMoveDirection(Direction.Axis.X);
    }
}
