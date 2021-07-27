package io.izzel.arclight.common.mixin.core.world.entity.animal;

import io.izzel.arclight.common.mod.util.ArclightCaptures;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.world.entity.animal.Fox;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(Fox.FoxEatBerriesGoal.class)
public abstract class Fox_EatBerriesGoalMixin extends MoveToBlockGoal {

    @SuppressWarnings("target") @Shadow(aliases = {"this$0", "f_28672_"}, remap = false)
    private Fox outerThis;

    public Fox_EatBerriesGoalMixin(PathfinderMob creature, double speedIn, int length) {
        super(creature, speedIn, length);
    }

    @Inject(method = "pickSweetBerries", cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", remap = false, target = "Ljava/util/Random;nextInt(I)I"))
    private void arclight$eatBerry(BlockState state, CallbackInfo ci) {
        if (CraftEventFactory.callEntityChangeBlockEvent(outerThis, this.blockPos, state.setValue(SweetBerryBushBlock.AGE, 1)).isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "pickGlowBerry", at = @At("HEAD"))
    private void arclight$pickGlowBerryPre(BlockState p_148927_, CallbackInfo ci) {
        ArclightCaptures.captureEntityChangeBlock(outerThis);
    }

    @Inject(method = "pickGlowBerry", at = @At("RETURN"))
    private void arclight$pickGlowBerryPost(BlockState p_148927_, CallbackInfo ci) {
        ArclightCaptures.getEntityChangeBlock();
    }
}
