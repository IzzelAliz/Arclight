package io.izzel.arclight.common.mixin.core.world.entity.ai.goal;

import io.izzel.arclight.common.bridge.core.world.entity.PathfinderMobBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MoveToBlockGoal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MoveToBlockGoal.class)
public abstract class MoveToBlockGoalMixin extends Goal {
    @Shadow @Final protected PathfinderMob mob;
    @Shadow protected BlockPos blockPos;

    @Override public void stop() {
        super.stop();
        this.blockPos = BlockPos.ZERO;
        ((PathfinderMobBridge) this.mob).bridge$setMovingTarget(null);
    }

    @Inject(method = "findNearestBlock", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/ai/goal/MoveToBlockGoal;blockPos:Lnet/minecraft/core/BlockPos;", opcode = 181, shift = At.Shift.AFTER))
    private void arclight$movingTarget(CallbackInfoReturnable<Boolean> cir) {
        ((PathfinderMobBridge) this.mob).bridge$setMovingTarget(this.blockPos == BlockPos.ZERO ? null : this.blockPos.immutable());
    }
}
