package io.izzel.arclight.common.mixin.core.entity.ai.goal;

import io.izzel.arclight.common.bridge.entity.MobEntityBridge;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.goal.HurtByTargetGoal;
import org.bukkit.event.entity.EntityTargetEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HurtByTargetGoal.class)
public class HurtByTargetGoalMixin extends TargetGoalMixin {

    @Inject(method = "startExecuting", at = @At("HEAD"))
    public void arclight$reason1(CallbackInfo ci) {
        ((MobEntityBridge) this.goalOwner).bridge$pushGoalTargetReason(EntityTargetEvent.TargetReason.TARGET_ATTACKED_ENTITY, true);
    }

    @Inject(method = "setAttackTarget", at = @At("HEAD"))
    public void arclight$reason2(MobEntity mobIn, LivingEntity targetIn, CallbackInfo ci) {
        ((MobEntityBridge) mobIn).bridge$pushGoalTargetReason(EntityTargetEvent.TargetReason.TARGET_ATTACKED_NEARBY_ENTITY, true);
    }
}
