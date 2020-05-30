package io.izzel.arclight.common.mixin.core.entity.monster;

import io.izzel.arclight.common.bridge.entity.LivingEntityBridge;
import net.minecraft.entity.Entity;
import net.minecraft.entity.monster.CaveSpiderEntity;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CaveSpiderEntity.class)
public abstract class CaveSpiderEntityMixin extends SpiderEntityMixin {

    @Inject(method = "attackEntityAsMob", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;addPotionEffect(Lnet/minecraft/potion/EffectInstance;)Z"))
    private void arclight$potionAttack(Entity entityIn, CallbackInfoReturnable<Boolean> cir) {
        ((LivingEntityBridge) entityIn).bridge$pushEffectCause(EntityPotionEffectEvent.Cause.ATTACK);
    }
}
