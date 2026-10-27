package io.izzel.arclight.forge.mixin.core.world.entity;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.izzel.arclight.common.bridge.core.world.entity.LivingEntityBridge;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Map;

@Mixin(LivingEntity.class)
public abstract class LivingEntityBulkEffectsMixin_Forge {

    // Forge copies activeEffects, clears it, then calls onEffectsRemoved. Its
    // own removal event remains in that notification method; do not duplicate it.
    @WrapOperation(method = "removeAllEffects()Z",
        at = @At(value = "INVOKE", target = "Ljava/util/Map;clear()V"),
        require = 1, expect = 1, allow = 1)
    private void arclight$removeAllowedEffects(Map<Holder<MobEffect>, MobEffectInstance> activeEffects,
                                              Operation<Void> original,
                                              @Local Map<Holder<MobEffect>, MobEffectInstance> snapshot) {
        for (Map.Entry<Holder<MobEffect>, MobEffectInstance> entry : snapshot.entrySet()) {
            MobEffectInstance effect = entry.getValue();
            // Earlier/nested callbacks may remove or replace another candidate.
            if (activeEffects.get(entry.getKey()) != effect) continue;
            if (((LivingEntityBridge) this).bridge$willRemoveEffect(effect)
                && activeEffects.get(entry.getKey()) == effect) {
                activeEffects.remove(entry.getKey());
            }
        }
        // Common filters the following onEffectsRemoved collection and computes
        // the boolean from the real map delta, including the all-vetoed case.
    }
}
