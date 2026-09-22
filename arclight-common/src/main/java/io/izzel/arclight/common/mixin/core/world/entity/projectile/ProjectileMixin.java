package io.izzel.arclight.common.mixin.core.world.entity.projectile;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import io.izzel.arclight.common.mixin.core.world.entity.EntityMixin;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
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
    @Shadow public abstract boolean deflect(ProjectileDeflection projectileDeflection, @org.jetbrains.annotations.Nullable Entity entity, EntityReference<Entity> entityReference, boolean bl);
    // @formatter:on

    @WrapOperation(method = "spawnProjectile(Lnet/minecraft/world/entity/projectile/Projectile;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Ljava/util/function/Consumer;)Lnet/minecraft/world/entity/projectile/Projectile;",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private static boolean arclight$captureSpawnResult(ServerLevel level, Entity entity, Operation<Boolean> original,
            @Share("arclight$spawnAccepted") LocalBooleanRef accepted) {
        boolean result = original.call(level, entity);
        accepted.set(result);
        return result;
    }

    @WrapOperation(method = "spawnProjectile(Lnet/minecraft/world/entity/projectile/Projectile;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Ljava/util/function/Consumer;)Lnet/minecraft/world/entity/projectile/Projectile;",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/Projectile;applyOnProjectileSpawned(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;)V"))
    private static void arclight$spawnEffectsAfterSuccess(Projectile projectile, ServerLevel level, ItemStack stack,
            Operation<Void> original, @Share("arclight$spawnAccepted") LocalBooleanRef accepted) {
        if (accepted.get()) {
            original.call(projectile, level, stack);
        }
    }

    @Inject(method = "setOwner", at = @At("RETURN"))
    private void arclight$updateSource(EntityReference<Entity> entityReference, CallbackInfo ci) {
        Entity entityIn = this.getOwner();
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

    @Inject(method = "hitTargetOrDeflectSelf", cancellable = true, at = @At("HEAD"))
    private void arclight$hitEvent(HitResult hitResult, CallbackInfoReturnable<ProjectileDeflection> cir) {
        org.bukkit.event.entity.ProjectileHitEvent event = CraftEventFactory.callProjectileHitEvent((Projectile) (Object) this, hitResult);
        this.hitCancelled = event != null && event.isCancelled();
        if (!(hitResult.getType() == HitResult.Type.BLOCK || !this.hitCancelled)) {
            cir.setReturnValue(ProjectileDeflection.NONE);
        }
    }
}
