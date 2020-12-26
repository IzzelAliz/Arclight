package io.izzel.arclight.common.mixin.core.world;

import io.izzel.arclight.common.bridge.world.IBlockReaderBridge;
import net.minecraft.block.BlockState;
import net.minecraft.fluid.IFluidState;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.IBlockReader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.Nullable;
import java.util.function.BiFunction;
import java.util.function.Function;

@Mixin(IBlockReader.class)
public interface IBlockReaderMixin extends IBlockReaderBridge {

    // @formatter:off
    @Shadow BlockState getBlockState(BlockPos pos);
    @Shadow IFluidState getFluidState(BlockPos pos);
    @Shadow @Nullable BlockRayTraceResult rayTraceBlocks(Vec3d p_217296_1_, Vec3d p_217296_2_, BlockPos p_217296_3_, VoxelShape p_217296_4_, BlockState p_217296_5_);
    @Shadow static <T> T doRayTrace(RayTraceContext context, BiFunction<RayTraceContext, BlockPos, T> rayTracer, Function<RayTraceContext, T> missFactory) { return null; }
    // @formatter:on

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    default BlockRayTraceResult rayTraceBlocks(RayTraceContext context) {
        class Func implements Function<RayTraceContext, BlockRayTraceResult> {

            @Override public BlockRayTraceResult apply(RayTraceContext rayTraceContext) {
                Vec3d vec3d = rayTraceContext.getStartVec().subtract(rayTraceContext.getEndVec());
                return BlockRayTraceResult.createMiss(rayTraceContext.getEndVec(), Direction.getFacingFromVector(vec3d.x, vec3d.y, vec3d.z), new BlockPos(rayTraceContext.getEndVec()));
            }
        }
        return doRayTrace(context, this::rayTraceBlock, new Func());
    }

    default BlockRayTraceResult rayTraceBlock(RayTraceContext context, BlockPos pos) {
        BlockState blockstate = this.bridge$getBlockStateIfLoaded(pos);
        if (blockstate == null) {
            Vec3d vec3d = context.getStartVec().subtract(context.getEndVec());
            return BlockRayTraceResult.createMiss(context.getEndVec(), Direction.getFacingFromVector(vec3d.x, vec3d.y, vec3d.z), new BlockPos(context.getEndVec()));
        }
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

    @Override
    @Nullable
    default BlockState bridge$getBlockStateIfLoaded(BlockPos pos) {
        return this.getBlockState(pos);
    }
}
