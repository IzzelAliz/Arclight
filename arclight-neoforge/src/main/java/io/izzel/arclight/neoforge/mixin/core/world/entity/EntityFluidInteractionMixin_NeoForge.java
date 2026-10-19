package io.izzel.arclight.neoforge.mixin.core.world.entity;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityFluidInteraction;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EntityFluidInteraction.class)
public abstract class EntityFluidInteractionMixin_NeoForge {

    // The exact predicate overload owns the scan; the boolean overload merely
    // delegates. Preserve NeoForge's FluidType tracker, predicate and push path.
    // The getType call follows the fluid/box intersection test but precedes
    // getFluidType, tracker lookup, predicate and optional getFlow.
    @ModifyExpressionValue(method = "update(Lnet/minecraft/world/entity/Entity;Ljava/util/function/Predicate;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/material/FluidState;getType()Lnet/minecraft/world/level/material/Fluid;"))
    private Fluid arclight$captureLavaContact(Fluid original, Entity entity, @Local MutableBlockPos mutablePos) {
        if (original.is(FluidTags.LAVA)) {
            ((EntityBridge) entity).bridge$setLastLavaContact(mutablePos.immutable());
        }
        return original;
    }
}
