package io.izzel.arclight.common.bridge.core.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public interface NoteBlockBridge {

    void bridge$play(Level worldIn, BlockPos pos, BlockState state);
}
