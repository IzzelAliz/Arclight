package io.izzel.arclight.common.mixin.core.world.level.block;

import io.izzel.arclight.mixin.Eject;
import io.izzel.arclight.common.bridge.core.world.level.WorldBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(StemBlock.class)
public class StemBlockMixin {

    @Redirect(method = "randomTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/RandomSource;nextInt(I)I", ordinal = 0))
    private int arclight$stemRate(RandomSource random, int bound, BlockState state, ServerLevel level, BlockPos pos) {
        var config = ((WorldBridge) level).bridge$spigotConfig();
        int modifier = (Object) this == Blocks.PUMPKIN_STEM ? config.pumpkinModifier : config.melonModifier;
        return arclight$allowStem(modifier, bound, random) ? 0 : 1;
    }

    private static boolean arclight$allowStem(int modifier, int bound, RandomSource random) {
        return random.nextFloat() < modifier / (100.0f * bound);
    }

    @Eject(method = "randomTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    public boolean arclight$cropGrow1(ServerLevel world, BlockPos pos, BlockState newState, int flags, CallbackInfo ci) {
        if (!CraftEventFactory.handleBlockGrowEvent(world, pos, newState, flags)) {
            ci.cancel();
        }
        return true;
    }

    @Eject(method = "randomTick", at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/server/level/ServerLevel;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    public boolean arclight$cropGrow2(ServerLevel world, BlockPos pos, BlockState state, CallbackInfo ci) {
        if (!CraftEventFactory.handleBlockGrowEvent(world, pos, state, Block.UPDATE_ALL)) {
            ci.cancel();
        }
        return true;
    }

    @Redirect(method = "performBonemeal", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    public boolean arclight$cropGrow3(ServerLevel world, BlockPos pos, BlockState newState, int flags) {
        return CraftEventFactory.handleBlockGrowEvent(world, pos, newState, flags);
    }
}
