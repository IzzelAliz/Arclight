package io.izzel.arclight.common.mixin.vanilla.world.entity;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Holder;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityFluidInteraction;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EntityFluidInteraction.class)
public abstract class EntityFluidInteractionMixin_Vanilla {

    // The typeHolder call is inside the nonempty/intersecting-fluid branch, before
    // tracker lookup and the ignoreCurrent/getFlow branch. Capture even if the
    // entity has no matching tracker or does not receive fluid push this tick.
    @ModifyExpressionValue(method = "update(Lnet/minecraft/world/entity/Entity;Z)Z",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/material/FluidState;typeHolder()Lnet/minecraft/core/Holder;"))
    private Holder<Fluid> arclight$captureLavaContact(Holder<Fluid> original, Entity entity, @Local MutableBlockPos mutablePos) {
        if (original.is(FluidTags.LAVA)) {
            ((EntityBridge) entity).bridge$setLastLavaContact(mutablePos.immutable());
        }
        return original;
    }
}
