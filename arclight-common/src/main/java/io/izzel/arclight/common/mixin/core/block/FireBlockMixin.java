package io.izzel.arclight.common.mixin.core.block;

import io.izzel.arclight.common.bridge.block.FireBlockBridge;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FireBlock;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.craftbukkit.v.block.CraftBlockState;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

@Mixin(FireBlock.class)
public abstract class FireBlockMixin implements FireBlockBridge {

    // @formatter:off
    @Shadow protected abstract BlockState getStateForPlacement(IBlockReader blockReader, BlockPos pos);
    @Shadow @Final private Object2IntMap<net.minecraft.block.Block> flammabilities;
    // @formatter:on

    @Redirect(method = "tick", at = @At(value = "INVOKE", ordinal = 1, target = "Lnet/minecraft/world/server/ServerWorld;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z"))
    public boolean arclight$fireSpread(ServerWorld world, BlockPos mutablePos, BlockState newState, int flags,
                                       BlockState state, ServerWorld worldIn, BlockPos pos) {
        if (world.getBlockState(mutablePos).getBlock() != Blocks.FIRE) {
            if (!CraftEventFactory.callBlockIgniteEvent(world, mutablePos, pos).isCancelled()) {
                return CraftEventFactory.handleBlockSpreadEvent(world, pos, mutablePos, newState, flags);
            }
        }
        return false;
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/server/ServerWorld;removeBlock(Lnet/minecraft/util/math/BlockPos;Z)Z"))
    public boolean arclight$extinguish1(ServerWorld world, BlockPos pos, boolean isMoving) {
        if (!CraftEventFactory.callBlockFadeEvent(world, pos, Blocks.AIR.getDefaultState()).isCancelled()) {
            world.removeBlock(pos, isMoving);
        }
        return false;
    }

    @Inject(method = "tryCatchFire", cancellable = true, at = @At(value = "INVOKE", ordinal = 1, target = "Lnet/minecraft/world/World;getBlockState(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/block/BlockState;"))
    public void arclight$blockBurn(World worldIn, BlockPos pos, int chance, Random random, int age, Direction face, CallbackInfo ci) {
        Block theBlock = CraftBlock.at(worldIn, pos);
        Block sourceBlock = CraftBlock.at(worldIn, pos.offset(face));
        BlockBurnEvent event = new BlockBurnEvent(theBlock, sourceBlock);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            ci.cancel();
        }
    }

    @Redirect(method = "updatePostPlacement", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/Block;getDefaultState()Lnet/minecraft/block/BlockState;"))
    public BlockState arclight$blockFade(net.minecraft.block.Block block, BlockState stateIn, Direction facing, BlockState facingState, IWorld worldIn, BlockPos currentPos, BlockPos facingPos) {
        if (!(worldIn instanceof World)) {
            return Blocks.AIR.getDefaultState();
        }
        CraftBlockState blockState = CraftBlockState.getBlockState(worldIn, currentPos);
        blockState.setData(Blocks.AIR.getDefaultState());
        BlockFadeEvent event = new BlockFadeEvent(blockState.getBlock(), blockState);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return this.getStateForPlacement(worldIn, currentPos).with(FireBlock.AGE, stateIn.get(FireBlock.AGE));
        } else {
            return blockState.getHandle();
        }
    }

    @Override
    public boolean bridge$canBurn(net.minecraft.block.Block block) {
        return this.flammabilities.containsKey(block);
    }
}
