package io.izzel.arclight.common.mixin.core.world.level.block;

import io.izzel.arclight.common.mod.util.ArclightCaptures;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.NetherFungusBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.TreeType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NetherFungusBlock.class)
public class FungusBlockMixin {

    @SuppressWarnings("ConstantConditions")
    @Inject(method = "performBonemeal", at = @At("HEAD"))
    private void arclight$captureTree(ServerLevel worldIn, RandomSource rand, BlockPos pos, BlockState state, CallbackInfo ci) {
        if ((Object) this == Blocks.WARPED_FUNGUS) {
            ArclightCaptures.captureTreeType(TreeType.WARPED_FUNGUS);
        } else if ((Object) this == Blocks.CRIMSON_FUNGUS) {
            ArclightCaptures.captureTreeType(TreeType.CRIMSON_FUNGUS);
        }
    }



    // Native Optional lambda is reached only for the final, eligible nonnull net.minecraft.core.Holder.
    // Keep opaque placement continuation and every argument exactly once; native discards its boolean.
    @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(method = "lambda$performBonemeal$0", at = @At(value = "INVOKE", target =
        "Lnet/minecraft/world/level/levelgen/feature/ConfiguredFeature;place(Lnet/minecraft/world/level/WorldGenLevel;Lnet/minecraft/world/level/chunk/ChunkGenerator;Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/BlockPos;)Z"))
    private static boolean arclight$finalHolder(net.minecraft.world.level.levelgen.feature.ConfiguredFeature<?, ?> feature, net.minecraft.world.level.WorldGenLevel level,
            net.minecraft.world.level.chunk.ChunkGenerator generator, RandomSource random, BlockPos pos, com.llamalad7.mixinextras.injector.wrapoperation.Operation<Boolean> original,
            @com.llamalad7.mixinextras.sugar.Local(argsOnly = true) net.minecraft.core.Holder<net.minecraft.world.level.levelgen.feature.ConfiguredFeature<?, ?>> holder) {
        io.izzel.arclight.common.mod.util.TreeCapture.selectGrowth(holder, false);
        return original.call(feature, level, generator, random, pos);
    }
}
