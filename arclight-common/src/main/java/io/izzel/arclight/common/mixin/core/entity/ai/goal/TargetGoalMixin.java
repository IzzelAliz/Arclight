package io.izzel.arclight.common.mixin.core.entity.ai.goal;

import io.izzel.arclight.common.bridge.entity.MobEntityBridge;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.goal.TargetGoal;
import org.bukkit.event.entity.EntityTargetEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TargetGoal.class)
public class TargetGoalMixin {

    @Shadow @Final protected MobEntity goalOwner;

    @Inject(method = "shouldContinueExecuting", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/MobEntity;setAttackTarget(Lnet/minecraft/entity/LivingEntity;)V"))
    private void arclight$reason(CallbackInfoReturnable<Boolean> cir) {
        ((MobEntityBridge) this.goalOwner).bridge$pushGoalTargetReason(EntityTargetEvent.TargetReason.CLOSEST_ENTITY, true);
    }

    @Inject(method = "resetTask", at = @At("HEAD"))
    private void arclight$reason(CallbackInfo ci) {
        ((MobEntityBridge) this.goalOwner).bridge$pushGoalTargetReason(EntityTargetEvent.TargetReason.FORGOT_TARGET, true);
    }
}
