package io.izzel.arclight.common.mixin.core.block;

import io.izzel.arclight.common.mod.util.ArclightCaptures;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.MushroomBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;
import org.bukkit.TreeType;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Random;

@Mixin(MushroomBlock.class)
public class MushroomBlockMixin {

    @Redirect(method = "randomTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/server/ServerWorld;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z"))
    public boolean arclight$blockSpread(ServerWorld world, BlockPos toPos, BlockState newState, int flags, BlockState state, ServerWorld worldIn, BlockPos fromPos) {
        return CraftEventFactory.handleBlockSpreadEvent(world, fromPos, toPos, newState, flags);
    }

    @SuppressWarnings("ConstantConditions")
    @Inject(method = "grow(Lnet/minecraft/world/server/ServerWorld;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Ljava/util/Random;)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/gen/feature/ConfiguredFeature;generate(Lnet/minecraft/world/ISeedReader;Lnet/minecraft/world/gen/ChunkGenerator;Ljava/util/Random;Lnet/minecraft/util/math/BlockPos;)Z"))
    private void arclight$captureTree(ServerWorld world, BlockPos pos, BlockState state, Random rand, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this == Blocks.BROWN_MUSHROOM) {
            ArclightCaptures.captureTreeType(TreeType.BROWN_MUSHROOM);
        } else if ((Object) this == Blocks.RED_MUSHROOM) {
            ArclightCaptures.captureTreeType(TreeType.RED_MUSHROOM);
        }
    }
}
