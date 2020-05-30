package io.izzel.arclight.common.bridge.world;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceContext;

public interface IBlockReaderBridge {

    BlockRayTraceResult bridge$rayTraceBlock(RayTraceContext context, BlockPos pos);
}
