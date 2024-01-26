package io.izzel.arclight.common.bridge.core.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;

public interface LavaFluidBridge {

    default BlockState bridge$forge$fireFluidPlaceBlockEvent(LevelAccessor level, BlockPos pos, BlockPos liquidPos, BlockState state) {
        return state;
    }

    boolean bridge$forge$isFlammable(LevelReader level, BlockPos pos, Direction face);
}
