package io.izzel.arclight.common.mixin.core.block;

import io.izzel.arclight.common.mod.util.ArclightCaptures;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FungusBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.server.ServerWorld;
import org.bukkit.TreeType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

@Mixin(FungusBlock.class)
public class FungusBlockMixin {

    @SuppressWarnings("ConstantConditions")
    @Inject(method = "grow", at = @At("HEAD"))
    private void arclight$captureTree(ServerWorld worldIn, Random rand, BlockPos pos, BlockState state, CallbackInfo ci) {
        if ((Object) this == Blocks.WARPED_FUNGUS) {
            ArclightCaptures.captureTreeType(TreeType.WARPED_FUNGUS);
        } else if ((Object) this == Blocks.CRIMSON_FUNGUS) {
            ArclightCaptures.captureTreeType(TreeType.CRIMSON_FUNGUS);
        }
    }
}
