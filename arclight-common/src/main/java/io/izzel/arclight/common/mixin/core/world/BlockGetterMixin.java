package io.izzel.arclight.common.mixin.core.world;

import io.izzel.arclight.common.bridge.core.world.IBlockReaderBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.Nullable;

@Mixin(BlockGetter.class)
public interface BlockGetterMixin extends IBlockReaderBridge {

    // @formatter:off
    @Shadow BlockState getBlockState(BlockPos pos);
    @Shadow FluidState getFluidState(BlockPos pos);
    @Shadow @Nullable BlockHitResult clipWithInteractionOverride(Vec3 startVec, Vec3 endVec, BlockPos pos, VoxelShape shape, BlockState state);
    // @formatter:on

    default BlockHitResult clip(ClipContext context, BlockPos pos) {
        BlockState blockstate = this.getBlockState(pos);
        FluidState ifluidstate = this.getFluidState(pos);
        Vec3 vec3d = context.getFrom();
        Vec3 vec3d1 = context.getFrom();
        VoxelShape voxelshape = context.getBlockShape(blockstate, (BlockGetter) this, pos);
        BlockHitResult blockraytraceresult = this.clipWithInteractionOverride(vec3d, vec3d1, pos, voxelshape, blockstate);
        VoxelShape voxelshape1 = context.getFluidShape(ifluidstate, (BlockGetter) this, pos);
        BlockHitResult blockraytraceresult1 = voxelshape1.clip(vec3d, vec3d1, pos);
        double d0 = blockraytraceresult == null ? Double.MAX_VALUE : context.getFrom().distanceToSqr(blockraytraceresult.getLocation());
        double d1 = blockraytraceresult1 == null ? Double.MAX_VALUE : context.getFrom().distanceToSqr(blockraytraceresult1.getLocation());
        return d0 <= d1 ? blockraytraceresult : blockraytraceresult1;
    }

    @Override
    default BlockHitResult bridge$rayTraceBlock(ClipContext context, BlockPos pos) {
        return clip(context, pos);
    }
}
