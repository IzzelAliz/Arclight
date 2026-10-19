package io.izzel.arclight.common.mixin.core.world.level.block;

import io.izzel.arclight.common.bridge.core.world.level.WorldBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(CocoaBlock.class)
public class CocoaBlockMixin {

    @Redirect(method = "randomTick(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/util/RandomSource;nextInt(I)I"), require = 1, expect = 1, allow = 1)
    private int arclight$cocoaGrowthChance(RandomSource receiver, int bound, BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        return receiver.nextFloat() < ((WorldBridge) level).bridge$spigotConfig().cocoaModifier / (100.0f * 5) ? 0 : 1;
    }

    @Redirect(method = "randomTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    public boolean arclight$blockGrow1(ServerLevel world, BlockPos pos, BlockState newState, int flags) {
        return CraftEventFactory.handleBlockGrowEvent(world, pos, newState, flags);
    }

    @Redirect(method = "performBonemeal", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private boolean arclight$blockGrow2(ServerLevel world, BlockPos pos, BlockState newState, int flags) {
        return CraftEventFactory.handleBlockGrowEvent(world, pos, newState, flags);
    }
}
