package io.izzel.arclight.common.mixin.core.world.level.block;

import io.izzel.arclight.common.mod.util.ArclightCaptures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.MultifaceSpreader;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(MultifaceSpreader.class)
public class MultifaceSpreaderMixin {

    @Inject(method = "getSpreadFromFaceTowardDirection", at = @At("RETURN"))
    private void arclight$captureSource(BlockState p_221613_, BlockGetter p_221614_, BlockPos pos, Direction p_221616_, Direction p_221617_, MultifaceSpreader.SpreadPredicate p_221618_, CallbackInfoReturnable<Optional<MultifaceSpreader.SpreadPos>> cir) {
        if (cir.getReturnValue().isPresent()) {
            ArclightCaptures.captureSpreadSource(pos);
        }
    }

    @Inject(method = "spreadToFace", at = @At("RETURN"))
    private void arclight$resetSource(LevelAccessor p_221594_, MultifaceSpreader.SpreadPos p_221595_, boolean p_221596_, CallbackInfoReturnable<Optional<MultifaceSpreader.SpreadPos>> cir) {
        ArclightCaptures.resetSpreadSource();
    }
}
