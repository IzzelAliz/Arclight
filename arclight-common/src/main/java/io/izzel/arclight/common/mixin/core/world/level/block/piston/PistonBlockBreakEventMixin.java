package io.izzel.arclight.common.mixin.core.world.level.block.piston;

import io.izzel.arclight.common.mod.server.event.ArclightEventFactory.BlockDestruction;
import io.izzel.arclight.common.mod.util.DistValidate;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PistonBaseBlock.class)
public abstract class PistonBlockBreakEventMixin {

    @Decorate(method = "moveBlocks(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;Z)Z",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/piston/PistonBaseBlock;dropResources(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;)V"), require = 1, allow = 1)
    private void arclight$blockBreakSource(BlockState state, LevelAccessor level, BlockPos pos, BlockEntity blockEntity,
                                         Level hostLevel, BlockPos pistonPos, Direction direction, boolean extending) throws Throwable {
        var previous = io.izzel.arclight.common.mod.util.ArclightCaptures.blockDestruction;
        BlockDestruction context = DistValidate.isValid(level)
            ? BlockDestruction.blockBreak(level, pos, pistonPos) : null;
        if (context != null) io.izzel.arclight.common.mod.util.ArclightCaptures.blockDestruction = context;
        try {
            DecorationOps.callsite().invoke(state, level, pos, blockEntity);
        } finally {
            if (context != null) {
                io.izzel.arclight.common.mod.util.ArclightCaptures.blockDestruction = previous;
            }
        }
    }
}
