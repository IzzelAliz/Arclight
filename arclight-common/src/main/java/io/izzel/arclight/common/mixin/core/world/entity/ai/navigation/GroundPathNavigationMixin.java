package io.izzel.arclight.common.mixin.core.world.entity.ai.navigation;

import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.level.pathfinder.Path;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(GroundPathNavigation.class)
public abstract class GroundPathNavigationMixin extends PathNavigationMixin {

    @Decorate(method = "createPath(Lnet/minecraft/world/entity/Entity;I)Lnet/minecraft/world/level/pathfinder/Path;",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/navigation/GroundPathNavigation;createPath(Lnet/minecraft/core/BlockPos;I)Lnet/minecraft/world/level/pathfinder/Path;"),
        require = 1, allow = 1)
    private Path arclight$groundEntityPathTarget(GroundPathNavigation navigation, BlockPos pos, int reachRange,
                                               @Local(ordinal = 0) Entity target) throws Throwable {
        Entity previousTarget = this.arclight$pathfindTarget;
        this.arclight$pathfindTarget = target;
        try {
            // Keep ground surface normalization and WallClimberNavigation's virtual block-path override.
            return (Path) DecorationOps.callsite().invoke(navigation, pos, reachRange);
        } finally {
            this.arclight$pathfindTarget = previousTarget;
        }
    }
}
