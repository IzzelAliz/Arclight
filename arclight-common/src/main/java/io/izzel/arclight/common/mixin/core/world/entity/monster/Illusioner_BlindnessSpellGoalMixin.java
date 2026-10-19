package io.izzel.arclight.common.mixin.core.world.entity.monster;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.izzel.arclight.common.bridge.core.world.entity.LivingEntityBridge;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "net.minecraft.world.entity.monster.illager.Illusioner$IllusionerBlindnessSpellGoal")
public class Illusioner_BlindnessSpellGoalMixin {
    @WrapOperation(method = "performSpellCasting", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean arclight$attackCause(LivingEntity target, MobEffectInstance effect, Entity source, Operation<Boolean> original) {
        LivingEntityBridge bridge = (LivingEntityBridge) target;
        EntityPotionEffectEvent.Cause previous = bridge.bridge$getEffectCause().orElse(null);
        bridge.bridge$pushEffectCause(EntityPotionEffectEvent.Cause.ATTACK);
        try {
            return original.call(target, effect, source);
        } finally {
            bridge.bridge$getEffectCause();
            bridge.bridge$pushEffectCause(previous);
        }
    }
}
