package io.izzel.arclight.common.mixin.core.entity.passive;

import net.minecraft.block.BlockState;
import net.minecraft.block.SweetBerryBushBlock;
import net.minecraft.entity.CreatureEntity;
import net.minecraft.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.entity.passive.FoxEntity;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(FoxEntity.EatBerriesGoal.class)
public abstract class FoxEntity_EatBerriesGoalMixin extends MoveToBlockGoal {

    @SuppressWarnings("target") @Shadow(aliases = {"this$0", "field_220732_h"}, remap = false)
    private FoxEntity outerThis;

    public FoxEntity_EatBerriesGoalMixin(CreatureEntity creature, double speedIn, int length) {
        super(creature, speedIn, length);
    }

    @Inject(method = "eatBerry", cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", remap = false, target = "Ljava/util/Random;nextInt(I)I"))
    private void arclight$eatBerry(CallbackInfo ci, BlockState state) {
        if (CraftEventFactory.callEntityChangeBlockEvent(outerThis, this.destinationBlock, state.with(SweetBerryBushBlock.AGE, 1)).isCancelled()) {
            ci.cancel();
        }
    }
}
