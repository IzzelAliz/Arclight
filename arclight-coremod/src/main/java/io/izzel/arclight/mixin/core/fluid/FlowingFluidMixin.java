package io.izzel.arclight.mixin.core.fluid;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.fluid.FlowingFluid;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.IFluidState;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.craftbukkit.v.block.data.CraftBlockData;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.FluidLevelChangeEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FlowingFluid.class)
public abstract class FlowingFluidMixin {

    // @formatter:off
    @Shadow protected abstract boolean canFlow(IBlockReader worldIn, BlockPos fromPos, BlockState fromBlockState, Direction direction, BlockPos toPos, BlockState toBlockState, IFluidState toFluidState, Fluid fluidIn);
    @Shadow protected abstract IFluidState calculateCorrectFlowingState(IWorldReader worldIn, BlockPos pos, BlockState blockStateIn);
    @Shadow protected abstract int func_215667_a(World p_215667_1_, BlockPos p_215667_2_, IFluidState p_215667_3_, IFluidState p_215667_4_);
    @Shadow protected abstract void flowAround(IWorld worldIn, BlockPos pos, IFluidState stateIn);
    // @formatter:on

    @Inject(method = "flowAround", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/fluid/FlowingFluid;flowInto(Lnet/minecraft/world/IWorld;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Lnet/minecraft/util/Direction;Lnet/minecraft/fluid/IFluidState;)V"))
    public void arclight$flowInto(IWorld worldIn, BlockPos pos, IFluidState stateIn, CallbackInfo ci) {
        Block source = CraftBlock.at(worldIn, pos);
        BlockFromToEvent event = new BlockFromToEvent(source, BlockFace.DOWN);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            ci.cancel();
        }
    }

    @Redirect(method = "func_207937_a", at = @At(value = "INVOKE", target = "Lnet/minecraft/fluid/FlowingFluid;canFlow(Lnet/minecraft/world/IBlockReader;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Lnet/minecraft/util/Direction;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Lnet/minecraft/fluid/IFluidState;Lnet/minecraft/fluid/Fluid;)Z"))
    public boolean arclight$flowInto(FlowingFluid flowingFluid, IBlockReader worldIn, BlockPos fromPos, BlockState fromBlockState, Direction direction, BlockPos toPos, BlockState toBlockState, IFluidState toFluidState, Fluid fluidIn) {
        if (this.canFlow(worldIn, fromPos, fromBlockState, direction, toPos, toBlockState, toFluidState, fluidIn)) {
            Block source = CraftBlock.at(((World) worldIn), fromPos);
            BlockFromToEvent event = new BlockFromToEvent(source, CraftBlock.notchToBlockFace(direction));
            Bukkit.getPluginManager().callEvent(event);
            return !event.isCancelled();
        } else {
            return false;
        }
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public void tick(World worldIn, BlockPos pos, IFluidState state) {
        if (!state.isSource()) {
            IFluidState ifluidstate = this.calculateCorrectFlowingState(worldIn, pos, worldIn.getBlockState(pos));
            int i = this.func_215667_a(worldIn, pos, state, ifluidstate);
            if (ifluidstate.isEmpty()) {
                state = ifluidstate;
                FluidLevelChangeEvent event = CraftEventFactory.callFluidLevelChangeEvent(worldIn, pos, Blocks.AIR.getDefaultState());
                if (event.isCancelled()) {
                    return;
                }
                worldIn.setBlockState(pos, ((CraftBlockData) event.getNewData()).getState(), 3);
            } else if (!ifluidstate.equals(state)) {
                state = ifluidstate;
                BlockState blockstate = ifluidstate.getBlockState();
                FluidLevelChangeEvent event = CraftEventFactory.callFluidLevelChangeEvent(worldIn, pos, blockstate);
                if (event.isCancelled()) {
                    return;
                }
                worldIn.setBlockState(pos, ((CraftBlockData) event.getNewData()).getState(), 2);
                worldIn.getPendingFluidTicks().scheduleTick(pos, ifluidstate.getFluid(), i);
                worldIn.notifyNeighborsOfStateChange(pos, blockstate.getBlock());
            }
        }

        this.flowAround(worldIn, pos, state);
    }
}
