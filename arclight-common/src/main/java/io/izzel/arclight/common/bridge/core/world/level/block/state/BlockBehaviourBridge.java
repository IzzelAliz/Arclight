package io.izzel.arclight.common.bridge.core.world.level.block.state;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public interface BlockBehaviourBridge {

    default boolean bridge$forge$canDropFromExplosion(BlockState state, BlockGetter level, BlockPos pos, Explosion explosion) {
        return state.getBlock().dropFromExplosion(explosion);
    }

    default void bridge$forge$onBlockExploded(BlockState state, Level level, BlockPos pos, Explosion explosion) {
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        state.getBlock().wasExploded(level, pos, explosion);
    }
}
