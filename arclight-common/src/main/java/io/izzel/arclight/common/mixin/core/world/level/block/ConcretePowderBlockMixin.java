package io.izzel.arclight.common.mixin.core.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.ConcretePowderBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.block.CraftBlockState;
import org.bukkit.craftbukkit.v.block.CraftBlockStates;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.block.BlockFormEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ConcretePowderBlock.class)
public abstract class ConcretePowderBlockMixin extends FallingBlockMixin {

    // @formatter:off
    @Shadow @Final private BlockState concrete;
    // @formatter:on

    @Redirect(method = "onLand", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    public boolean arclight$blockForm(Level world, BlockPos pos, BlockState newState, int flags) {
        return CraftEventFactory.handleBlockFormEvent(world, pos, newState, flags);
    }

    @Redirect(method = "getStateForPlacement", at = @At(value = "FIELD", target = "Lnet/minecraft/world/level/block/ConcretePowderBlock;concrete:Lnet/minecraft/world/level/block/state/BlockState;"))
    public BlockState arclight$blockForm(ConcretePowderBlock instance, BlockPlaceContext context) {
        Level world = context.getLevel();
        BlockPos blockPos = context.getClickedPos();
        CraftBlockState blockState = CraftBlockStates.getBlockState(world, blockPos);
        blockState.setData(this.concrete);
        BlockFormEvent event = new BlockFormEvent(blockState.getBlock(), blockState);
        Bukkit.getPluginManager().callEvent(event);
        if (!event.isCancelled()) {
            return blockState.getHandle();
        }
        return super.getStateForPlacement(context);
    }

    @Redirect(method = "updateShape", at = @At(value = "FIELD", target = "Lnet/minecraft/world/level/block/ConcretePowderBlock;concrete:Lnet/minecraft/world/level/block/state/BlockState;"))
    public BlockState arclight$blockForm(ConcretePowderBlock instance, BlockState stateIn, Direction facing, BlockState facingState, LevelAccessor worldIn, BlockPos currentPos, BlockPos facingPos) {
        if (!(worldIn instanceof Level)) {
            return this.concrete;
        }
        CraftBlockState blockState = CraftBlockStates.getBlockState(worldIn, currentPos);
        blockState.setData(this.concrete);
        BlockFormEvent event = new BlockFormEvent(blockState.getBlock(), blockState);
        Bukkit.getPluginManager().callEvent(event);
        if (!event.isCancelled()) {
            return blockState.getHandle();
        }
        return super.updateShape(stateIn, facing, facingState, worldIn, currentPos, facingPos);
    }
}
