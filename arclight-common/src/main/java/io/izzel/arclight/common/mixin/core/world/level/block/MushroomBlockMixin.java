package io.izzel.arclight.common.mixin.core.world.level.block;

import io.izzel.arclight.common.mod.util.ArclightCaptures;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.MushroomBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.TreeType;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MushroomBlock.class)
public class MushroomBlockMixin {

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
}
