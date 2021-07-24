package io.izzel.arclight.common.mixin.core.world.level.block;

import com.google.common.collect.ImmutableList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.piston.PistonStructureResolver;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.event.block.BlockPistonEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.AbstractList;
import java.util.List;

@Mixin(PistonBaseBlock.class)
public class PistonBlockMixin {

    // @formatter:off
    @Shadow @Final private boolean isSticky;
    // @formatter:on

    @Inject(method = "checkIfExtend", cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;blockEvent(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/Block;II)V"))
    public void arclight$pistonRetract(Level worldIn, BlockPos pos, BlockState state, CallbackInfo ci, Direction direction) {
        if (!this.isSticky) {
            Block block = CraftBlock.at(worldIn, pos);
            BlockPistonRetractEvent event = new BlockPistonRetractEvent(block, ImmutableList.of(), CraftBlock.notchToBlockFace(direction));
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "moveBlocks", cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD,
        at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/world/level/block/piston/PistonStructureResolver;getToDestroy()Ljava/util/List;"))
    public void arclight$pistonAction(Level worldIn, BlockPos pos, Direction directionIn, boolean extending, CallbackInfoReturnable<Boolean> cir,
                                      BlockPos blockPos, PistonStructureResolver helper) {
        final Block craftBlock = CraftBlock.at(worldIn, pos);

        final List<BlockPos> moved = helper.getToPush();
        final List<BlockPos> broken = helper.getToDestroy();

        class BlockList extends AbstractList<Block> {

            @Override
            public int size() {
                return moved.size() + broken.size();
            }

            @Override
            public org.bukkit.block.Block get(int index) {
                if (index >= size() || index < 0) {
                    throw new ArrayIndexOutOfBoundsException(index);
                }
                BlockPos pos = index < moved.size() ? moved.get(index) : broken.get(index - moved.size());
                return craftBlock.getWorld().getBlockAt(pos.getX(), pos.getY(), pos.getZ());
            }
        }

        List<Block> blocks = new BlockList();

        Direction direction = extending ? directionIn : directionIn.getOpposite();
        BlockPistonEvent event;
        if (extending) {
            event = new BlockPistonExtendEvent(craftBlock, blocks, CraftBlock.notchToBlockFace(direction));
        } else {
            event = new BlockPistonRetractEvent(craftBlock, blocks, CraftBlock.notchToBlockFace(direction));
        }
        Bukkit.getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            for (BlockPos b : broken) {
                worldIn.sendBlockUpdated(b, Blocks.AIR.defaultBlockState(), worldIn.getBlockState(b), 3);
            }
            for (BlockPos b : moved) {
                worldIn.sendBlockUpdated(b, Blocks.AIR.defaultBlockState(), worldIn.getBlockState(b), 3);
                b = b.relative(direction);
                worldIn.sendBlockUpdated(b, Blocks.AIR.defaultBlockState(), worldIn.getBlockState(b), 3);
            }
            cir.setReturnValue(false);
        }
    }
}
