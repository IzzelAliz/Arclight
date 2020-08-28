package io.izzel.arclight.common.mixin.core.entity.projectile;

import io.izzel.arclight.common.bridge.entity.projectile.DamagingProjectileEntityBridge;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.projectile.DamagingProjectileEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(DamagingProjectileEntity.class)
public abstract class DamagingProjectileEntityMixin extends ProjectileEntityMixin implements DamagingProjectileEntityBridge {

    // @formatter:off
    @Shadow public double accelerationX;
    @Shadow public double accelerationY;
    @Shadow public double accelerationZ;
    // @formatter:on

    public float bukkitYield;
    public boolean isIncendiary;

    @Inject(method = "<init>(Lnet/minecraft/entity/EntityType;Lnet/minecraft/world/World;)V", at = @At("RETURN"))
    private void arclight$init(EntityType<? extends DamagingProjectileEntity> p_i50173_1_, World p_i50173_2_, CallbackInfo ci) {
        this.bukkitYield = 1;
        this.isIncendiary = true;
    }

    public void setDirection(double d0, double d1, double d2) {
        double d3 = MathHelper.sqrt(d0 * d0 + d1 * d1 + d2 * d2);

        this.accelerationX = d0 / d3 * 0.1D;
        this.accelerationY = d1 / d3 * 0.1D;
        this.accelerationZ = d2 / d3 * 0.1D;
    }

    @Inject(method = "tick", locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/entity/projectile/DamagingProjectileEntity;onImpact(Lnet/minecraft/util/math/RayTraceResult;)V"))
    private void arclight$projectileHit(CallbackInfo ci, Entity entity, RayTraceResult rayTraceResult) {
        if (this.removed) {
            CraftEventFactory.callProjectileHitEvent((DamagingProjectileEntity) (Object) this, rayTraceResult);
        }
    }

    @Inject(method = "attackEntityFrom", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;getLookVec()Lnet/minecraft/util/math/vector/Vector3d;"))
    private void arclight$nonLivingAttack(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (CraftEventFactory.handleNonLivingEntityDamageEvent((DamagingProjectileEntity) (Object) this, source, amount)) {
            cir.setReturnValue(false);
        }
    }

    @Override
    public void bridge$setBukkitYield(float yield) {
        this.bukkitYield = yield;
    }
}
