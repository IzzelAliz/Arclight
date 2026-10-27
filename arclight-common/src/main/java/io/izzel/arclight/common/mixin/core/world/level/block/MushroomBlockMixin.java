package io.izzel.arclight.common.mixin.core.world.level.block;

import io.izzel.arclight.common.mod.util.ArclightCaptures;
import io.izzel.arclight.common.bridge.core.world.level.WorldBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.MushroomBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.TreeType;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MushroomBlock.class)
public class MushroomBlockMixin {

    @Redirect(method = "randomTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/RandomSource;nextInt(I)I", ordinal = 0))
    private int arclight$mushroomRate(RandomSource random, int bound, BlockState state, ServerLevel level, BlockPos pos) {
        return arclight$allowMushroom(((WorldBridge) level).bridge$spigotConfig().mushroomModifier, random) ? 0 : 1;
    }

    private static boolean arclight$allowMushroom(int modifier, RandomSource random) {
        return random.nextFloat() < modifier / (100.0f * 25);
    }

    @Redirect(method = "randomTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    public boolean arclight$blockSpread(ServerLevel world, BlockPos toPos, BlockState newState, int flags, BlockState state, ServerLevel worldIn, BlockPos fromPos) {
        return CraftEventFactory.handleBlockSpreadEvent(world, fromPos, toPos, newState, flags);
    }

    @SuppressWarnings("ConstantConditions")
    @Inject(method = "growMushroom", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/feature/ConfiguredFeature;place(Lnet/minecraft/world/level/WorldGenLevel;Lnet/minecraft/world/level/chunk/ChunkGenerator;Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/BlockPos;)Z"))
    private void arclight$captureTree(ServerLevel world, BlockPos pos, BlockState state, RandomSource rand, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this == Blocks.BROWN_MUSHROOM) {
            ArclightCaptures.captureTreeType(TreeType.BROWN_MUSHROOM);
        } else if ((Object) this == Blocks.RED_MUSHROOM) {
            ArclightCaptures.captureTreeType(TreeType.RED_MUSHROOM);
        }
    }


    @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(
        method = "growMushroom(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/util/RandomSource;)Z",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/core/Holder;value()Ljava/lang/Object;"),
        require = 1, expect = 1, allow = 1)
    private Object arclight$observeMushroomFeature(net.minecraft.core.Holder<?> holder,
            com.llamalad7.mixinextras.injector.wrapoperation.Operation<Object> original,
            @com.llamalad7.mixinextras.sugar.Local(argsOnly = true) ServerLevel level) {
        // Record the actual eligible holder, but do not select block-vs-feature event policy.
        @SuppressWarnings("unchecked")
        var featureHolder = (net.minecraft.core.Holder<net.minecraft.world.level.levelgen.feature.ConfiguredFeature<?, ?>>) holder;
        io.izzel.arclight.common.mod.util.TreeCapture.observeMushroom(this);
        return original.call(holder);
    }

}
