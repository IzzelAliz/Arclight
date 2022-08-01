package io.izzel.arclight.common.mixin.core.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.NyliumBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NyliumBlock.class)
public class NyliumBlockMixin {

    @Inject(method = "randomTick", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    private void arclight$blockFade(BlockState p_221835_, ServerLevel level, BlockPos pos, RandomSource p_221838_, CallbackInfo ci) {
        if (CraftEventFactory.callBlockFadeEvent(level, pos, Blocks.NETHERRACK.defaultBlockState()).isCancelled()) {
            ci.cancel();
        }
    }
}
