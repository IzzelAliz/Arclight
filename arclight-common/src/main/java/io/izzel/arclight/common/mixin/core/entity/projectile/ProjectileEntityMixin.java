package io.izzel.arclight.common.mixin.core.entity.projectile;

import io.izzel.arclight.common.bridge.entity.EntityBridge;
import io.izzel.arclight.common.mixin.core.entity.EntityMixin;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import org.bukkit.craftbukkit.v.entity.CraftEntity;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.projectiles.ProjectileSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

@Mixin(ProjectileEntity.class)
public abstract class ProjectileEntityMixin extends EntityMixin {

    // @formatter:off
    @Shadow @Nullable public abstract Entity getShooter();
    @Shadow protected void onImpact(RayTraceResult result) { }
    // @formatter:on

    @Inject(method = "setShooter", at = @At("RETURN"))
    private void arclight$updateSource(Entity entityIn, CallbackInfo ci) {
        if (entityIn != null) {
            CraftEntity entity = ((EntityBridge) entityIn).bridge$getBukkitEntity();
            if (entity instanceof ProjectileSource) {
                this.projectileSource = ((ProjectileSource) entity);
            }
        }
    }

    private boolean hitCancelled = false;

    @Inject(method = "onImpact", cancellable = true, at = @At("HEAD"))
    private void arclight$onHit(RayTraceResult result, CallbackInfo ci) {
        ProjectileHitEvent event = CraftEventFactory.callProjectileHitEvent((ProjectileEntity) (Object) this, result);
        hitCancelled = event != null && event.isCancelled();
        if (!(result.getType() == RayTraceResult.Type.BLOCK || !hitCancelled)) {
            ci.cancel();
        }
    }

    @Inject(method = "func_230299_a_", cancellable = true, at = @At("HEAD"))
    private void arclight$cancelBlockHit(BlockRayTraceResult result, CallbackInfo ci) {
        if (hitCancelled) {
            ci.cancel();
        }
    }
}
