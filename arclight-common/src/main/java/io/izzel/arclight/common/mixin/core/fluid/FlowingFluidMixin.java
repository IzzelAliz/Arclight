package io.izzel.arclight.common.mixin.core.fluid;

import io.izzel.arclight.common.mod.util.DistValidate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
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
    @Shadow protected abstract boolean canSpreadTo(BlockGetter worldIn, BlockPos fromPos, BlockState fromBlockState, Direction direction, BlockPos toPos, BlockState toBlockState, FluidState toFluidState, Fluid fluidIn);
    // @formatter:on

    @Inject(method = "spread", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/material/FlowingFluid;spreadTo(Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;Lnet/minecraft/world/level/material/FluidState;)V"))
    public void arclight$flowInto(LevelAccessor worldIn, BlockPos pos, FluidState stateIn, CallbackInfo ci) {
        if (!DistValidate.isValid(worldIn)) return;
        Block source = CraftBlock.at(worldIn, pos);
        BlockFromToEvent event = new BlockFromToEvent(source, BlockFace.DOWN);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            ci.cancel();
        }
    }

    @Redirect(method = "spreadToSides", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/material/FlowingFluid;canSpreadTo(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/material/FluidState;Lnet/minecraft/world/level/material/Fluid;)Z"))
    public boolean arclight$flowInto(FlowingFluid flowingFluid, BlockGetter worldIn, BlockPos fromPos, BlockState fromBlockState, Direction direction, BlockPos toPos, BlockState toBlockState, FluidState toFluidState, Fluid fluidIn) {
        if (this.canSpreadTo(worldIn, fromPos, fromBlockState, direction, toPos, toBlockState, toFluidState, fluidIn)) {
            if (!DistValidate.isValid(worldIn)) return true;
            Block source = CraftBlock.at(((Level) worldIn), fromPos);
            BlockFromToEvent event = new BlockFromToEvent(source, CraftBlock.notchToBlockFace(direction));
            Bukkit.getPluginManager().callEvent(event);
            return !event.isCancelled();
        } else {
            return false;
        }
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private boolean arclight$fluidLevelChange(Level world, BlockPos pos, BlockState newState, int flags) {
        if (!DistValidate.isValid(world)) return world.setBlock(pos, newState, flags);
        FluidLevelChangeEvent event = CraftEventFactory.callFluidLevelChangeEvent(world, pos, newState);
        if (event.isCancelled()) {
            return false;
        } else {
            return world.setBlock(pos, ((CraftBlockData) event.getNewData()).getState(), flags);
        }
    }
}
