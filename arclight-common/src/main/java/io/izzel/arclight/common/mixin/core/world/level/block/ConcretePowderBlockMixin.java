package io.izzel.arclight.common.mixin.core.world.level.block;

import io.izzel.arclight.common.bridge.core.world.IWorldBridge;
import io.izzel.arclight.common.mod.server.event.ArclightEventFactory;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ConcretePowderBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.block.CraftBlockState;
import org.bukkit.craftbukkit.v.block.CraftBlockStates;
import org.bukkit.event.block.BlockFormEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ConcretePowderBlock.class)
public abstract class ConcretePowderBlockMixin extends FallingBlockMixin {

    // @formatter:off
    @Shadow @Final private Block concrete;
    // @formatter:on

    @Decorate(method = "onLand", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    public boolean arclight$blockForm(Level world, BlockPos pos, BlockState newState, int flags) throws Throwable {
        if (IWorldBridge.from(world) instanceof IWorldBridge bridge) {
            final var event = ArclightEventFactory.callBlockFormEvent(bridge.bridge$getMinecraftWorld(), pos, newState, flags, null);
            if (event != null) {
                if (event.isCancelled()) {
                    return false;
                }
                newState = ((CraftBlockState) event.getNewState()).getHandle();
            }
        }
        return (boolean) DecorationOps.callsite().invoke(world, pos, newState, flags);
    }

    @Redirect(method = "getStateForPlacement", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/Block;defaultBlockState()Lnet/minecraft/world/level/block/state/BlockState;"))
    public BlockState arclight$blockForm(Block instance, BlockPlaceContext context) {
        Level world = context.getLevel();
        BlockPos blockPos = context.getClickedPos();
        CraftBlockState blockState = CraftBlockStates.getBlockState(world, blockPos);
        blockState.setData(this.concrete.defaultBlockState());
        BlockFormEvent event = new BlockFormEvent(blockState.getBlock(), blockState);
        Bukkit.getPluginManager().callEvent(event);
        if (!event.isCancelled()) {
            return blockState.getHandle();
        }
        return super.getStateForPlacement(context);
    }

    @Redirect(method = "updateShape", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/Block;defaultBlockState()Lnet/minecraft/world/level/block/state/BlockState;"))
    public BlockState arclight$blockForm(Block instance, BlockState stateIn, Direction facing, BlockState facingState, LevelAccessor worldIn, BlockPos currentPos, BlockPos facingPos) {
        if (!(worldIn instanceof Level)) {
            return this.concrete.defaultBlockState();
        }
        CraftBlockState blockState = CraftBlockStates.getBlockState(worldIn, currentPos);
        blockState.setData(this.concrete.defaultBlockState());
        BlockFormEvent event = new BlockFormEvent(blockState.getBlock(), blockState);
        Bukkit.getPluginManager().callEvent(event);
        if (!event.isCancelled()) {
            return blockState.getHandle();
        }
        return super.updateShape(stateIn, facing, facingState, worldIn, currentPos, facingPos);
    }
}
