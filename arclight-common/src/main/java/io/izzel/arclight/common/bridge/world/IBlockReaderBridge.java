package io.izzel.arclight.common.bridge.world;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;

public interface IBlockReaderBridge {

    BlockHitResult bridge$rayTraceBlock(ClipContext context, BlockPos pos);
}
