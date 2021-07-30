package io.izzel.arclight.common.mixin.core.world.entity.projectile;

import io.izzel.arclight.common.bridge.core.entity.projectile.DamagingProjectileEntityBridge;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(AbstractHurtingProjectile.class)
public abstract class AbstractHurtingProjectileMixin extends ProjectileMixin implements DamagingProjectileEntityBridge {

    // @formatter:off
    @Shadow public double xPower;
    @Shadow public double yPower;
    @Shadow public double zPower;
    // @formatter:on

    public float bukkitYield;
    public boolean isIncendiary;

    @Inject(method = "<init>(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/level/Level;)V", at = @At("RETURN"))
    private void arclight$init(EntityType<? extends AbstractHurtingProjectile> p_i50173_1_, Level p_i50173_2_, CallbackInfo ci) {
        this.bukkitYield = 1;
        this.isIncendiary = true;
    }

    public void setDirection(double d0, double d1, double d2) {
        double d3 = Math.sqrt(d0 * d0 + d1 * d1 + d2 * d2);

        this.xPower = d0 / d3 * 0.1D;
        this.yPower = d1 / d3 * 0.1D;
        this.zPower = d2 / d3 * 0.1D;
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/AbstractHurtingProjectile;onHit(Lnet/minecraft/world/phys/HitResult;)V"))
    private void arclight$preOnHit(AbstractHurtingProjectile abstractHurtingProjectile, HitResult hitResult) {
        this.preOnHit(hitResult);
    }

    @Inject(method = "tick", locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/world/entity/projectile/AbstractHurtingProjectile;onHit(Lnet/minecraft/world/phys/HitResult;)V"))
    private void arclight$projectileHit(CallbackInfo ci, Entity entity, HitResult rayTraceResult) {
        if (this.isRemoved()) {
            CraftEventFactory.callProjectileHitEvent((AbstractHurtingProjectile) (Object) this, rayTraceResult);
        }
    }

    @Inject(method = "hurt", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getLookAngle()Lnet/minecraft/world/phys/Vec3;"))
    private void arclight$nonLivingAttack(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (CraftEventFactory.handleNonLivingEntityDamageEvent((AbstractHurtingProjectile) (Object) this, source, amount, false)) {
            cir.setReturnValue(false);
        }
    }

    @Override
    public void bridge$setBukkitYield(float yield) {
        this.bukkitYield = yield;
    }
}
