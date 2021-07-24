package io.izzel.arclight.common.mixin.core.world.level.block;

import io.izzel.arclight.common.mod.util.ArclightCaptures;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FungusBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.TreeType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

@Mixin(FungusBlock.class)
public class FungusBlockMixin {

    @SuppressWarnings("ConstantConditions")
    @Inject(method = "performBonemeal", at = @At("HEAD"))
    private void arclight$captureTree(ServerLevel worldIn, Random rand, BlockPos pos, BlockState state, CallbackInfo ci) {
        if ((Object) this == Blocks.WARPED_FUNGUS) {
            ArclightCaptures.captureTreeType(TreeType.WARPED_FUNGUS);
        } else if ((Object) this == Blocks.CRIMSON_FUNGUS) {
            ArclightCaptures.captureTreeType(TreeType.CRIMSON_FUNGUS);
        }
    }
}
