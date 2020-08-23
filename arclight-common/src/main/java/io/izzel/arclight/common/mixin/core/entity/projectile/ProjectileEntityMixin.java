package io.izzel.arclight.common.mixin.core.entity.projectile;

import io.izzel.arclight.common.bridge.entity.EntityBridge;
import io.izzel.arclight.common.mixin.core.entity.EntityMixin;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.util.math.RayTraceResult;
import org.bukkit.craftbukkit.v.entity.CraftEntity;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
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
    @Shadow @Nullable public abstract Entity func_234616_v_();
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

    @Inject(method = "onImpact", at = @At("HEAD"))
    private void arclight$onHit(RayTraceResult result, CallbackInfo ci) {
        CraftEventFactory.callProjectileHitEvent((ProjectileEntity) (Object) this, result);
    }
}
