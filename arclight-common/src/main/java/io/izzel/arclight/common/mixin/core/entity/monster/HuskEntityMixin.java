package io.izzel.arclight.common.mixin.core.entity.monster;

import io.izzel.arclight.common.bridge.entity.LivingEntityBridge;
import net.minecraft.entity.Entity;
import net.minecraft.entity.monster.HuskEntity;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HuskEntity.class)
public abstract class HuskEntityMixin extends ZombieEntityMixin {

    @Inject(method = "attackEntityAsMob", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;addPotionEffect(Lnet/minecraft/potion/EffectInstance;)Z"))
    private void arclight$reason(Entity entityIn, CallbackInfoReturnable<Boolean> cir) {
        ((LivingEntityBridge) entityIn).bridge$pushEffectCause(EntityPotionEffectEvent.Cause.ATTACK);
    }
}
