package io.izzel.arclight.common.mixin.core.world.level.material;

import io.izzel.arclight.common.mod.util.DistValidate;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.craftbukkit.v.block.data.CraftBlockData;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.FluidLevelChangeEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FlowingFluid.class)
public abstract class FlowingFluidMixin {

    // @formatter:off
    @Shadow protected abstract boolean canMaybePassThrough(BlockGetter level, BlockPos fromPos, BlockState fromBlockState, Direction direction, BlockPos toPos, BlockState toBlockState, FluidState toFluidState);
    @Shadow protected abstract FluidState getNewLiquid(ServerLevel level, BlockPos pos, BlockState state);
    @Shadow protected static boolean canHoldSpecificFluid(BlockGetter level, BlockPos pos, BlockState state, Fluid fluid) { throw new AssertionError(); }
    // @formatter:on

    @Inject(method = "spread", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/material/FlowingFluid;spreadTo(Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;Lnet/minecraft/world/level/material/FluidState;)V"))
    public void arclight$flowInto(ServerLevel worldIn, BlockPos pos, BlockState blockState, FluidState stateIn, CallbackInfo ci) {
        if (!DistValidate.isValid(worldIn)) return;
        Block source = CraftBlock.at(worldIn, pos);
        BlockFromToEvent event = new BlockFromToEvent(source, BlockFace.DOWN);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            ci.cancel();
        }
    }

    @Redirect(method = "getSpread", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/material/FlowingFluid;canMaybePassThrough(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/material/FluidState;)Z"))
    public boolean arclight$flowInto(FlowingFluid flowingFluid, BlockGetter level, BlockPos fromPos, BlockState fromBlockState, Direction direction, BlockPos toPos, BlockState toBlockState, FluidState toFluidState) {
        ServerLevel serverLevel = (ServerLevel) level;
        if (this.canMaybePassThrough(serverLevel, fromPos, fromBlockState, direction, toPos, toBlockState, toFluidState)) {
            FluidState newFluidState = this.getNewLiquid(serverLevel, toPos, toBlockState);
            if (!canHoldSpecificFluid(serverLevel, toPos, toBlockState, newFluidState.getType())) return false;
            if (!DistValidate.isValid(serverLevel)) return true;
            Block source = CraftBlock.at(serverLevel, fromPos);
            BlockFromToEvent event = new BlockFromToEvent(source, CraftBlock.notchToBlockFace(direction));
            Bukkit.getPluginManager().callEvent(event);
            return !event.isCancelled();
        } else {
            return false;
        }
    }

    @Decorate(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private boolean arclight$fluidLevelChange(ServerLevel world, BlockPos pos, BlockState newState, int flags) throws Throwable {
        if (DistValidate.isValid(world)) {
            FluidLevelChangeEvent event = CraftEventFactory.callFluidLevelChangeEvent(world, pos, newState);
            if (event.isCancelled()) {
                return (boolean) DecorationOps.cancel().invoke();
            } else {
                newState = ((CraftBlockData) event.getNewData()).getState();
            }
        }
        return (boolean) DecorationOps.callsite().invoke(world, pos, newState, flags);
    }
}
