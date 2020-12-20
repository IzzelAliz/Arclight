package io.izzel.arclight.common.bridge.world;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceContext;

import javax.annotation.Nullable;

public interface IBlockReaderBridge {

    BlockRayTraceResult bridge$rayTraceBlock(RayTraceContext context, BlockPos pos);

    @Nullable BlockState bridge$getBlockStateIfLoaded(BlockPos pos);
}
