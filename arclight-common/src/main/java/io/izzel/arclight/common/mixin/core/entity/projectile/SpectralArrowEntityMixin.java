package io.izzel.arclight.common.mixin.core.entity.projectile;

import io.izzel.arclight.common.bridge.entity.LivingEntityBridge;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.SpectralArrowEntity;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SpectralArrowEntity.class)
public abstract class SpectralArrowEntityMixin extends AbstractArrowEntityMixin {

    @Inject(method = "arrowHit", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;addPotionEffect(Lnet/minecraft/potion/EffectInstance;)Z"))
    private void arclight$hit(LivingEntity living, CallbackInfo ci) {
        ((LivingEntityBridge) living).bridge$pushEffectCause(EntityPotionEffectEvent.Cause.ARROW);
    }
}
