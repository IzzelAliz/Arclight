package io.izzel.arclight.common.mixin.core.world.level.block;

import io.izzel.arclight.common.bridge.core.world.level.WorldBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BambooSaplingBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BambooSaplingBlock.class)
public class BambooSaplingBlockMixin {

    @Redirect(method = "randomTick(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/RandomSource;nextInt(I)I"), require = 1, expect = 1, allow = 1)
    private int arclight$bambooGrowthChance(RandomSource receiver, int bound, BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        return receiver.nextFloat() < ((WorldBridge) level).bridge$spigotConfig().bambooModifier / (100.0f * 3) ? 0 : 1;
    }

    @Redirect(method = "growBamboo", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    public boolean arclight$blockSpread(Level world, BlockPos pos, BlockState newState, int flags) {
        return CraftEventFactory.handleBlockSpreadEvent(world, pos.below(), pos, newState, flags);
    }
}
