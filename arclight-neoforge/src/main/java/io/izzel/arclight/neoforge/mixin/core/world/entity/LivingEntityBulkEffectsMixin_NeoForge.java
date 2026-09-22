package io.izzel.arclight.neoforge.mixin.core.world.entity;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.izzel.arclight.common.bridge.core.world.entity.LivingEntityBridge;
import io.izzel.arclight.common.mod.util.BulkClearMapView;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Map;
import java.util.Set;

/**
 * NeoForge first asks EventHooks, then records allowed effects in a temporary
 * map and finally removes activeEffects by key. The first wrapper keeps that
 * EventHooks-to-temporary-map sequence intact; the second freezes the keys
 * whose values still match the temporary map just before physical removal.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityBulkEffectsMixin_NeoForge {

    @WrapOperation(method = "removeAllEffects",
        at = @At(value = "INVOKE", target = "Lnet/neoforged/neoforge/event/EventHooks;onEffectRemoved(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/effect/MobEffectInstance;)Z"))
    private boolean arclight$skipStaleEffectCallback(LivingEntity entity, MobEffectInstance effect,
                                                     Operation<Boolean> original) {
        // Earlier callbacks may replace or remove a later snapshot candidate.
        // Returning the veto value skips this candidate without notifying again.
        if (entity.getActiveEffectsMap().get(effect.getEffect()) != effect) {
            return true;
        }
        return original.call(entity, effect);
    }

    @WrapOperation(method = "removeAllEffects",
        at = @At(value = "INVOKE", target = "Ljava/util/Map;entrySet()Ljava/util/Set;"))
    private Set<Map.Entry<Holder<MobEffect>, MobEffectInstance>> arclight$snapshotEffectCandidates(
        Map<Holder<MobEffect>, MobEffectInstance> activeEffects,
        Operation<Set<Map.Entry<Holder<MobEffect>, MobEffectInstance>>> original) {
        return BulkClearMapView.immutableEntrySnapshot(original.call(activeEffects));
    }

    @WrapOperation(method = "removeAllEffects",
        at = @At(value = "INVOKE", target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"))
    private Object arclight$excludeBukkitCancelledEffects(Map<Holder<MobEffect>, MobEffectInstance> toRemove,
                                                           Object holder, Object effect,
                                                           Operation<Object> original) {
        if (!((LivingEntityBridge) this).bridge$willRemoveEffect((MobEffectInstance) effect)) {
            return null;
        }
        return original.call(toRemove, holder, effect);
    }

    @WrapOperation(method = "removeAllEffects",
        at = @At(value = "INVOKE", target = "Ljava/util/Map;keySet()Ljava/util/Set;"))
    private Set<Holder<MobEffect>> arclight$removeOnlyStillMatchingEffects(
        Map<Holder<MobEffect>, MobEffectInstance> toRemove, Operation<Set<Holder<MobEffect>>> original) {
        Set<Holder<MobEffect>> keys = original.call(toRemove);
        return BulkClearMapView.matchingKeys(keys, toRemove, ((LivingEntity) (Object) this).getActiveEffectsMap());
    }
}
