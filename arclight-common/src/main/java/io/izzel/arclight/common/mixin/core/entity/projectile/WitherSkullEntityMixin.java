package io.izzel.arclight.common.mixin.core.entity.projectile;

import io.izzel.arclight.common.bridge.entity.LivingEntityBridge;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.WitherSkullEntity;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;
import org.bukkit.Bukkit;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WitherSkullEntity.class)
public abstract class WitherSkullEntityMixin extends DamagingProjectileEntityMixin {

    @Inject(method = "onEntityHit", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;heal(F)V"))
    private void arclight$heal(EntityRayTraceResult result, CallbackInfo ci) {
        ((LivingEntityBridge) this.func_234616_v_()).bridge$pushHealReason(EntityRegainHealthEvent.RegainReason.WITHER);
    }

    @Inject(method = "onEntityHit", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;addPotionEffect(Lnet/minecraft/potion/EffectInstance;)Z"))
    private void arclight$effect(EntityRayTraceResult result, CallbackInfo ci) {
        ((LivingEntityBridge) result.getEntity()).bridge$pushEffectCause(EntityPotionEffectEvent.Cause.ATTACK);
    }

    @Redirect(method = "onImpact", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;createExplosion(Lnet/minecraft/entity/Entity;DDDFZLnet/minecraft/world/Explosion$Mode;)Lnet/minecraft/world/Explosion;"))
    private Explosion arclight$explode(World world, Entity entityIn, double xIn, double yIn, double zIn, float explosionRadius, boolean causesFire, Explosion.Mode modeIn) {
        ExplosionPrimeEvent event = new ExplosionPrimeEvent(this.getBukkitEntity(), explosionRadius, causesFire);
        Bukkit.getPluginManager().callEvent(event);
        if (!event.isCancelled()) {
            return this.world.createExplosion((WitherSkullEntity) (Object) this, xIn, yIn, zIn, event.getRadius(), event.getFire(), modeIn);
        }
        return null;
    }
}
