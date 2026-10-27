package io.izzel.arclight.common.bridge.core.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public interface BlockDestructionBridge {

    default boolean bridge$usesNeoForgeDestructionDrops() {
        return false;
    }

    default int bridge$getDestructionExperience(BlockState state, ServerLevel level, BlockPos pos, Entity breaker) {
        return ((BlockBridge) this).bridge$getExpDrop(state, level, pos, ItemStack.EMPTY);
    }
}
