package io.izzel.arclight.common.mixin.core.block;

import net.minecraft.block.BlockState;
import net.minecraft.block.ConcretePowderBlock;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.block.CraftBlockState;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.block.BlockFormEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ConcretePowderBlock.class)
public abstract class ConcretePowderBlockMixin extends FallingBlockMixin {

    // @formatter:off
    @Shadow @Final private BlockState solidifiedState;
    // @formatter:on

    @Redirect(method = "onEndFalling", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z"))
    public boolean arclight$blockForm(World world, BlockPos pos, BlockState newState, int flags) {
        return CraftEventFactory.handleBlockFormEvent(world, pos, newState, flags);
    }

    @Redirect(method = "getStateForPlacement", at = @At(value = "FIELD", target = "Lnet/minecraft/block/ConcretePowderBlock;solidifiedState:Lnet/minecraft/block/BlockState;"))
    public BlockState arclight$blockForm(@Coerce ConcretePowderBlockMixin block, BlockItemUseContext context) {
        World world = context.getWorld();
        BlockPos blockPos = context.getPos();
        CraftBlockState blockState = CraftBlockState.getBlockState(world, blockPos);
        blockState.setData(this.solidifiedState);
        BlockFormEvent event = new BlockFormEvent(blockState.getBlock(), blockState);
        Bukkit.getPluginManager().callEvent(event);
        if (!event.isCancelled()) {
            return blockState.getHandle();
        }
        return super.getStateForPlacement(context);
    }

    @Redirect(method = "updatePostPlacement", at = @At(value = "FIELD", target = "Lnet/minecraft/block/ConcretePowderBlock;solidifiedState:Lnet/minecraft/block/BlockState;"))
    public BlockState arclight$blockForm(@Coerce ConcretePowderBlockMixin block, BlockState stateIn, Direction facing, BlockState facingState, IWorld worldIn, BlockPos currentPos, BlockPos facingPos) {
        if (!(worldIn instanceof World)) {
            return this.solidifiedState;
        }
        CraftBlockState blockState = CraftBlockState.getBlockState(worldIn, currentPos);
        blockState.setData(this.solidifiedState);
        BlockFormEvent event = new BlockFormEvent(blockState.getBlock(), blockState);
        Bukkit.getPluginManager().callEvent(event);
        if (!event.isCancelled()) {
            return blockState.getHandle();
        }
        return super.updatePostPlacement(stateIn, facing, facingState, worldIn, currentPos, facingPos);
    }
}
