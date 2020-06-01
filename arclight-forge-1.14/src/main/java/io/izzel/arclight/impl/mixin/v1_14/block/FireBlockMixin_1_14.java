package io.izzel.arclight.impl.mixin.v1_14.block;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FireBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(FireBlock.class)
public class FireBlockMixin_1_14 {

    @Redirect(method = "tick", at = @At(value = "INVOKE", ordinal = 1, target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z"))
    public boolean arclight$fireSpread(World world, BlockPos mutablePos, BlockState newState, int flags,
                                       BlockState state, World worldIn, BlockPos pos) {
        if (world.getBlockState(mutablePos).getBlock() != Blocks.FIRE) {
            if (!CraftEventFactory.callBlockIgniteEvent(world, mutablePos, pos).isCancelled()) {
                return CraftEventFactory.handleBlockSpreadEvent(world, pos, mutablePos, newState, flags);
            }
        }
        return false;
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;removeBlock(Lnet/minecraft/util/math/BlockPos;Z)Z"))
    public boolean arclight$extinguish1(World world, BlockPos pos, boolean isMoving) {
        if (!CraftEventFactory.callBlockFadeEvent(world, pos, Blocks.AIR.getDefaultState()).isCancelled()) {
            world.removeBlock(pos, isMoving);
        }
        return false;
    }
}
