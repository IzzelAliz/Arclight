package io.izzel.arclight.common.mixin.core.world.level.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.DirtPathBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DirtPathBlock.class)
public class DirtPathBlockMixin {

    @Inject(method = "tick", cancellable = true, at = @At("HEAD"))
    private void arclight$checkValid(BlockState state, ServerLevel level, BlockPos pos, RandomSource p_221073_, CallbackInfo ci) {
        if (!state.canSurvive(level, pos)) {
            ci.cancel();
        }
    }
}
