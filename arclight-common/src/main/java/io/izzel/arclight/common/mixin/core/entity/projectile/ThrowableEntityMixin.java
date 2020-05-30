package io.izzel.arclight.common.mixin.core.entity.projectile;

import io.izzel.arclight.common.bridge.entity.LivingEntityBridge;
import io.izzel.arclight.common.mixin.core.entity.EntityMixin;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ThrowableEntity;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

@Mixin(ThrowableEntity.class)
public abstract class ThrowableEntityMixin extends EntityMixin {

    // @formatter:off
    @Shadow protected abstract void onImpact(RayTraceResult result);
    @Shadow @Nullable public abstract LivingEntity getThrower();
    // @formatter:on

    @Inject(method = "<init>(Lnet/minecraft/entity/EntityType;Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/world/World;)V", at = @At("RETURN"))
    private void arclight$init(EntityType<? extends ThrowableEntity> type, LivingEntity livingEntityIn, World worldIn, CallbackInfo ci) {
        this.projectileSource = ((LivingEntityBridge) livingEntityIn).bridge$getBukkitEntity();
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/projectile/ThrowableEntity;onImpact(Lnet/minecraft/util/math/RayTraceResult;)V"))
    private void arclight$projectileHit(ThrowableEntity entity, RayTraceResult result) {
        this.onImpact(result);
        if (this.removed) {
            CraftEventFactory.callProjectileHitEvent((ThrowableEntity) (Object) this, result);
        }
    }

}
