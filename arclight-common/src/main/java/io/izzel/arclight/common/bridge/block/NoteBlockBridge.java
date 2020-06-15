package io.izzel.arclight.common.bridge.block;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public interface NoteBlockBridge {

    void bridge$play(World worldIn, BlockPos pos, BlockState state);
}
