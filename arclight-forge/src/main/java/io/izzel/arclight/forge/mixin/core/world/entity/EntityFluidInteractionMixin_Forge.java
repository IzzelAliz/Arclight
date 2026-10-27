package io.izzel.arclight.forge.mixin.core.world.entity;

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
public abstract class EntityFluidInteractionMixin_Forge {

    // Capture the intersecting fluid before Forge's per-type tracker loop and
    // ignoreCurrent/canPushEntity gate. The second getType call is only a push check.
    @ModifyExpressionValue(method = "update(Lnet/minecraft/world/entity/Entity;Z)V",
        at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/world/level/material/FluidState;getType()Lnet/minecraft/world/level/material/Fluid;"),
        require = 1, expect = 1, allow = 1)
    private Fluid arclight$captureLavaContact(Fluid original, Entity entity, @Local MutableBlockPos mutablePos) {
        if (original.is(FluidTags.LAVA)) {
            ((EntityBridge) entity).bridge$setLastLavaContact(mutablePos.immutable());
        }
        return original;
    }
}
