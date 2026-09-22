package io.izzel.arclight.fabric.mixin.core.world.entity;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.izzel.arclight.common.mod.util.EffectRestoreMap;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Map;

/** Supplies a full snapshot view to Fabric's priority-1000 clear/restore wrapper. */
@Mixin(value = LivingEntity.class, priority = 1001)
public abstract class LivingEntityEffectRestoreMixin_Fabric {
    @WrapOperation(method = "removeAllEffects",
        at = @At(value = "INVOKE", target = "Ljava/util/Map;clear()V"))
    private void arclight$protectReentrantEffectRestoration(Map<?, ?> activeEffects, Operation<Void> original) {
        // Leave Fabric callbacks and the lower-priority Bukkit decision wrapper intact.
        original.call(new EffectRestoreMap<>(activeEffects));
    }
}
