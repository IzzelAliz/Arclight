package io.izzel.arclight.common.mixin.core.world.level.block;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import io.izzel.arclight.common.bridge.core.world.level.WorldBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.GrowingPlantHeadBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(GrowingPlantHeadBlock.class)
public class GrowingPlantHeadBlockMixin {

    @ModifyExpressionValue(method = "randomTick", at = @At(value = "FIELD",
        target = "Lnet/minecraft/world/level/block/GrowingPlantHeadBlock;growPerTickProbability:D"),
        require = 1, expect = 1, allow = 1)
    private double arclight$growthProbability(double original, BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        var config = ((WorldBridge) level).bridge$spigotConfig();
        int modifier;
        if ((Object) this == Blocks.KELP) {
            modifier = config.kelpModifier;
        } else if ((Object) this == Blocks.TWISTING_VINES) {
            modifier = config.twistingVinesModifier;
        } else if ((Object) this == Blocks.WEEPING_VINES) {
            modifier = config.weepingVinesModifier;
        } else {
            modifier = config.caveVinesModifier;
        }
        // Scale the incoming value before NeoForge's pre hook, without replacing either RNG.
        return (modifier / 100.0D) * original;
    }

    @Redirect(method = "randomTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    private boolean arclight$blockGrow(ServerLevel world, BlockPos to, BlockState state, BlockState state1, ServerLevel worldIn, BlockPos from, RandomSource random) {
        return CraftEventFactory.handleBlockSpreadEvent(world, from, to, state, Block.UPDATE_ALL);
    }
}
