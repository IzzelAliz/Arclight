package io.izzel.arclight.common.mixin.core.world.level.block;

import io.izzel.arclight.common.mod.util.ArclightCaptures;
import io.izzel.arclight.mixin.Eject;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.SculkSpreader;
import net.minecraft.world.level.block.SculkVeinBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SculkVeinBlock.class)
public class SculkVeinBlockMixin {

    @Inject(method = "attemptUseCharge", at = @At("HEAD"))
    private void arclight$captureSource(SculkSpreader.ChargeCursor p_222369_, LevelAccessor p_222370_, BlockPos source, RandomSource p_222372_, SculkSpreader p_222373_, boolean p_222374_, CallbackInfoReturnable<Integer> cir) {
        ArclightCaptures.captureSpreadSource(source);
    }

    @Inject(method = "attemptUseCharge", at = @At("RETURN"))
    private void arclight$resetSource(SculkSpreader.ChargeCursor p_222369_, LevelAccessor p_222370_, BlockPos source, RandomSource p_222372_, SculkSpreader p_222373_, boolean p_222374_, CallbackInfoReturnable<Integer> cir) {
        ArclightCaptures.resetSpreadSource();
    }

    @Eject(method = "attemptPlaceSculk", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/LevelAccessor;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private boolean arclight$blockSpread(LevelAccessor level, BlockPos pos, BlockState state, int i, CallbackInfoReturnable<Boolean> cir) {
        if (!CraftEventFactory.handleBlockSpreadEvent(level, ArclightCaptures.getSpreadPos(), pos, state, i)) {
            cir.setReturnValue(false);
            return false;
        }
        return true;
    }
}
