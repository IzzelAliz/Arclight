package io.izzel.arclight.common.mixin.core.world.entity.projectile;

import io.izzel.arclight.common.mixin.core.world.entity.EntityMixin;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileDeflection;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.bukkit.craftbukkit.v.entity.CraftEntity;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.projectiles.ProjectileSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

@Mixin(Projectile.class)
public abstract class ProjectileMixin extends EntityMixin {

    // @formatter:off
    @Shadow @Nullable public abstract Entity getOwner();
    @Shadow protected void onHit(HitResult result) { }
    @Shadow protected abstract ProjectileDeflection hitTargetOrDeflectSelf(HitResult hitResult);
    @Shadow public abstract boolean deflect(ProjectileDeflection projectileDeflection, @org.jetbrains.annotations.Nullable Entity entity, @org.jetbrains.annotations.Nullable Entity entity2, boolean bl);
    // @formatter:on

    @Inject(method = "setOwner", at = @At("RETURN"))
    private void arclight$updateSource(Entity entityIn, CallbackInfo ci) {
        if (entityIn != null) {
            CraftEntity entity = entityIn.bridge$getBukkitEntity();
            if (entity instanceof ProjectileSource) {
                this.projectileSource = ((ProjectileSource) entity);
            }
        }
    }

    protected boolean hitCancelled = false;

    @Inject(method = "onHitBlock", cancellable = true, at = @At("HEAD"))
    private void arclight$cancelBlockHit(BlockHitResult result, CallbackInfo ci) {
        if (hitCancelled) {
            ci.cancel();
        }
    }

    private transient boolean arclight$hitEventFired = false;

    @Inject(method = "hitTargetOrDeflectSelf", cancellable = true, at = @At("HEAD"))
    private void arclight$hitEvent(HitResult hitResult, CallbackInfoReturnable<ProjectileDeflection> cir) {
        if (!arclight$hitEventFired) {
            arclight$hitEventFired = true;
            org.bukkit.event.entity.ProjectileHitEvent event = CraftEventFactory.callProjectileHitEvent((Projectile) (Object) this, hitResult);
            this.hitCancelled = event != null && event.isCancelled();
            if (!(hitResult.getType() == HitResult.Type.BLOCK || !this.hitCancelled)) {
                cir.setReturnValue(ProjectileDeflection.NONE);
            }
        }
    }
}
