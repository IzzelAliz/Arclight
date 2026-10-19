package io.izzel.arclight.common.mixin.core.world.entity.animal;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(targets = "net.minecraft.world.entity.animal.bee.Bee$BeeGrowCropGoal")
public class Bee_GrowCropGoalMixin {
    @SuppressWarnings("target")
    @Shadow(aliases = {"this$0", "f_28021_", "field_20373"}, remap = false)
    private Bee outerThis;

    // Veto this layer's proposal at the native null guard, not the entire tick.
    // This leaves pre-event bonemeal intact and skips all three commit effects.
    @ModifyVariable(method = "tick()V", at = @At(value = "LOAD", ordinal = 0), ordinal = 1, require = 1)
    private BlockState arclight$entityChangeBlock(BlockState state, @Local BlockPos blockPos) {
        return state != null && CraftEventFactory.callEntityChangeBlockEvent(outerThis, blockPos, state) ? state : null;
    }
}
