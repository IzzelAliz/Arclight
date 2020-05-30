package io.izzel.arclight.common.mixin.core.enchantment;

import io.izzel.arclight.common.bridge.entity.LivingEntityBridge;
import net.minecraft.enchantment.DamageEnchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DamageEnchantment.class)
public class DamageEnchantmentMixin {

    @Inject(method = "onEntityDamaged", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;addPotionEffect(Lnet/minecraft/potion/EffectInstance;)Z"))
    public void arclight$entityDamage(LivingEntity user, Entity target, int level, CallbackInfo ci) {
        ((LivingEntityBridge) target).bridge$pushEffectCause(EntityPotionEffectEvent.Cause.ATTACK);
    }
}
