package io.izzel.arclight.common.mixin.core.entity.projectile;

import io.izzel.arclight.common.mixin.core.entity.EntityMixin;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FireworkRocketEntity.class)
public abstract class FireworkRocketEntityMixin extends EntityMixin {

    @Inject(method = "func_213893_k", cancellable = true, at = @At("HEAD"))
    private void arclight$fireworksExplode(CallbackInfo ci) {
        if (CraftEventFactory.callFireworkExplodeEvent((FireworkRocketEntity) (Object) this).isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "dealExplosionDamage", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;attackEntityFrom(Lnet/minecraft/util/DamageSource;F)Z"))
    private void arclight$damageSource(CallbackInfo ci) {
        CraftEventFactory.entityDamage = (FireworkRocketEntity) (Object) this;
    }

    @Inject(method = "dealExplosionDamage", at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/entity/LivingEntity;attackEntityFrom(Lnet/minecraft/util/DamageSource;F)Z"))
    private void arclight$damageSourceReset(CallbackInfo ci) {
        CraftEventFactory.entityDamage = null;
    }
}
