package io.izzel.arclight.common.mixin.core.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.RedstoneTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
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

    @Inject(method = "tick", cancellable = true, at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/server/level/ServerLevel;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private void arclight$blockRedstone1(BlockState state, ServerLevel worldIn, BlockPos pos, Random rand, CallbackInfo ci) {
        int oldCurrent = state.getValue(RedstoneTorchBlock.LIT) ? 15 : 0;
        if (oldCurrent != 0) {
            CraftBlock block = CraftBlock.at(worldIn, pos);
            BlockRedstoneEvent event = new BlockRedstoneEvent(block, oldCurrent, 0);
            Bukkit.getPluginManager().callEvent(event);
            if (event.getNewCurrent() != 0) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "tick", cancellable = true, at = @At(value = "INVOKE", ordinal = 1, target = "Lnet/minecraft/server/level/ServerLevel;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private void arclight$blockRedstone2(BlockState state, ServerLevel worldIn, BlockPos pos, Random rand, CallbackInfo ci) {
        int oldCurrent = state.getValue(RedstoneTorchBlock.LIT) ? 15 : 0;
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
