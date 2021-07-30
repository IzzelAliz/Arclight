package io.izzel.arclight.common.bridge.core.world;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;

public interface IBlockReaderBridge {

    BlockHitResult bridge$rayTraceBlock(ClipContext context, BlockPos pos);
}
