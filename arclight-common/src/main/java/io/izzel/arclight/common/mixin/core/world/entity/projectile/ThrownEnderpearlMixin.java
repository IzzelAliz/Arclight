package io.izzel.arclight.common.mixin.core.world.entity.projectile;

import io.izzel.arclight.common.bridge.core.world.damagesource.DamageSourceBridge;
import io.izzel.arclight.common.bridge.core.world.level.WorldBridge;
import io.izzel.arclight.common.bridge.core.world.level.portal.DimensionTransitionBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.world.level.portal.TeleportTransition;
import org.bukkit.event.player.PlayerTeleportEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.phys.HitResult;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ThrownEnderpearl.class)
public abstract class ThrownEnderpearlMixin extends ThrowableProjectileMixin {

    @Inject(method = "onHit", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/throwableitemprojectile/ThrownEnderpearl;discard()V"))
    private void arclight$hit(HitResult hitResult, CallbackInfo ci) {
        this.bridge$pushEntityRemoveCause(EntityRemoveEvent.Cause.HIT);
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/projectile/throwableitemprojectile/ThrownEnderpearl;discard()V"))
    private void arclight$despawn(CallbackInfo ci) {
        this.bridge$pushEntityRemoveCause(EntityRemoveEvent.Cause.DESPAWN);
    }

    @Inject(method = "onHit", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private void arclight$spawnEndermite(HitResult result, CallbackInfo ci) {
        ((WorldBridge) this.level()).bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.ENDER_PEARL);
    }

    @Decorate(method = "onHit", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;teleport(Lnet/minecraft/world/level/portal/TeleportTransition;)Lnet/minecraft/server/level/ServerPlayer;"))
    private ServerPlayer arclight$pearlCause(ServerPlayer player, TeleportTransition transition) throws Throwable {
        ((DimensionTransitionBridge) (Object) transition).bridge$setTeleportCause(PlayerTeleportEvent.TeleportCause.ENDER_PEARL);
        return (ServerPlayer) DecorationOps.callsite().invoke(player, transition);
    }

    @Redirect(method = "onHit", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private boolean arclight$entityDamage(ServerPlayer player, ServerLevel level, DamageSource source, float amount) {
        var damageSource = ((DamageSourceBridge) this.damageSources().enderPearl()).bridge$customCausingEntity((ThrownEnderpearl) (Object) this);
        return player.hurtServer(level, damageSource, amount);
    }
}
