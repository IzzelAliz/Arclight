package io.izzel.arclight.common.mixin.core.world.entity.projectile;

import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownLingeringPotion;
import net.minecraft.world.phys.HitResult;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.event.entity.LingeringPotionSplashEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(ThrownLingeringPotion.class)
public abstract class ThrownLingeringPotionMixin extends ThrowableItemProjectileMixin {

    @Unique private transient HitResult arclight$hitResult;

    @Inject(method = "onHitAsPotion", at = @At("HEAD"))
    private void arclight$captureHit(net.minecraft.server.level.ServerLevel level, net.minecraft.world.item.ItemStack stack, HitResult hitResult, CallbackInfo ci) {
        arclight$hitResult = hitResult;
    }

    @Inject(method = "onHit", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;discard()V"))
    private void arclight$hitCause(HitResult hitResult, CallbackInfo ci) {
        this.bridge$pushEntityRemoveCause(EntityRemoveEvent.Cause.HIT);
    }

    @Inject(method = "onHitAsPotion", cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD,
        at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private void arclight$makeCloud(net.minecraft.server.level.ServerLevel level, net.minecraft.world.item.ItemStack stack, HitResult hitResult, CallbackInfo ci, AreaEffectCloud cloud) {
        LingeringPotionSplashEvent event = CraftEventFactory.callLingeringPotionSplashEvent((ThrownLingeringPotion) (Object) this, arclight$hitResult, cloud);
        if (event.isCancelled() || cloud.isRemoved()) {
            ci.cancel();
            cloud.discard();
        }
    }

    @Inject(method = "onHitAsPotion", at = @At("RETURN"))
    private void arclight$resetResult(net.minecraft.server.level.ServerLevel level, net.minecraft.world.item.ItemStack stack, HitResult hitResult, CallbackInfo ci) {
        arclight$hitResult = null;
    }
}
