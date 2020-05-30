package io.izzel.arclight.common.mixin.core.block;

import net.minecraft.block.BlockState;
import net.minecraft.block.RedstoneTorchBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

@Mixin(RedstoneTorchBlock.class)
public class RedstoneTorchBlockMixin {

    @Inject(method = "update", cancellable = true, at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z"))
    private static void arclight$blockRedstone1(BlockState state, World worldIn, BlockPos pos, Random random, boolean flag, CallbackInfo ci) {
        int oldCurrent = state.get(RedstoneTorchBlock.LIT) ? 15 : 0;
        if (oldCurrent != 0) {
            CraftBlock block = CraftBlock.at(worldIn, pos);
            BlockRedstoneEvent event = new BlockRedstoneEvent(block, oldCurrent, 0);
            Bukkit.getPluginManager().callEvent(event);
            if (event.getNewCurrent() != 0) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "update", cancellable = true, at = @At(value = "INVOKE", ordinal = 1, target = "Lnet/minecraft/world/World;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z"))
    private static void arclight$blockRedstone2(BlockState state, World worldIn, BlockPos pos, Random random, boolean flag, CallbackInfo ci) {
        int oldCurrent = state.get(RedstoneTorchBlock.LIT) ? 15 : 0;
        if (oldCurrent != 15) {
            CraftBlock block = CraftBlock.at(worldIn, pos);
            BlockRedstoneEvent event = new BlockRedstoneEvent(block, oldCurrent, 15);
            Bukkit.getPluginManager().callEvent(event);
            if (event.getNewCurrent() != 15) {
                ci.cancel();
            }
        }
    }
}
