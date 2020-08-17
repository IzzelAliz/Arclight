package io.izzel.arclight.common.mixin.core.block;

import com.google.common.collect.ImmutableList;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.PistonBlock;
import net.minecraft.block.PistonBlockStructureHelper;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
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

@Mixin(PistonBlock.class)
public class PistonBlockMixin {

    // @formatter:off
    @Shadow @Final private boolean isSticky;
    // @formatter:on

    @Inject(method = "checkForMove", cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;addBlockEvent(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/Block;II)V"))
    public void arclight$pistonRetract(World worldIn, BlockPos pos, BlockState state, CallbackInfo ci, Direction direction) {
        if (!this.isSticky) {
            Block block = CraftBlock.at(worldIn, pos);
            BlockPistonRetractEvent event = new BlockPistonRetractEvent(block, ImmutableList.of(), CraftBlock.notchToBlockFace(direction));
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "doMove", cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD,
        at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/block/PistonBlockStructureHelper;getBlocksToDestroy()Ljava/util/List;"))
    public void arclight$pistonAction(World worldIn, BlockPos pos, Direction directionIn, boolean extending, CallbackInfoReturnable<Boolean> cir,
                                      BlockPos blockPos, PistonBlockStructureHelper helper) {
        final Block craftBlock = CraftBlock.at(worldIn, pos);

        final List<BlockPos> moved = helper.getBlocksToMove();
        final List<BlockPos> broken = helper.getBlocksToDestroy();

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
                worldIn.notifyBlockUpdate(b, Blocks.AIR.getDefaultState(), worldIn.getBlockState(b), 3);
            }
            for (BlockPos b : moved) {
                worldIn.notifyBlockUpdate(b, Blocks.AIR.getDefaultState(), worldIn.getBlockState(b), 3);
                b = b.offset(direction);
                worldIn.notifyBlockUpdate(b, Blocks.AIR.getDefaultState(), worldIn.getBlockState(b), 3);
            }
            cir.setReturnValue(false);
        }
    }
}
