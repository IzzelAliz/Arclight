package io.izzel.arclight.common.mixin.core.world;

import io.izzel.arclight.common.bridge.world.IBlockReaderBridge;
import net.minecraft.block.BlockState;
import net.minecraft.fluid.IFluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.IBlockReader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.Nullable;

@Mixin(IBlockReader.class)
public interface IBlockReaderMixin extends IBlockReaderBridge {

    // @formatter:off
    @Shadow BlockState getBlockState(BlockPos pos);
    @Shadow IFluidState getFluidState(BlockPos pos);
    @Shadow @Nullable BlockRayTraceResult rayTraceBlocks(Vec3d p_217296_1_, Vec3d p_217296_2_, BlockPos p_217296_3_, VoxelShape p_217296_4_, BlockState p_217296_5_);
    // @formatter:on

    default BlockRayTraceResult rayTraceBlock(RayTraceContext context, BlockPos pos) {
        BlockState blockstate = this.getBlockState(pos);
        IFluidState ifluidstate = this.getFluidState(pos);
        Vec3d vec3d = context.getStartVec();
        Vec3d vec3d1 = context.getStartVec();
        VoxelShape voxelshape = context.getBlockShape(blockstate, (IBlockReader) this, pos);
        BlockRayTraceResult blockraytraceresult = this.rayTraceBlocks(vec3d, vec3d1, pos, voxelshape, blockstate);
        VoxelShape voxelshape1 = context.getFluidShape(ifluidstate, (IBlockReader) this, pos);
        BlockRayTraceResult blockraytraceresult1 = voxelshape1.rayTrace(vec3d, vec3d1, pos);
        double d0 = blockraytraceresult == null ? Double.MAX_VALUE : context.getStartVec().squareDistanceTo(blockraytraceresult.getHitVec());
        double d1 = blockraytraceresult1 == null ? Double.MAX_VALUE : context.getStartVec().squareDistanceTo(blockraytraceresult1.getHitVec());
        return d0 <= d1 ? blockraytraceresult : blockraytraceresult1;
    }

    @Override
    default BlockRayTraceResult bridge$rayTraceBlock(RayTraceContext context, BlockPos pos) {
        return rayTraceBlock(context, pos);
    }
}
