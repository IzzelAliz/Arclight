package io.izzel.arclight.common.mixin.core.entity.ai.goal;

import io.izzel.arclight.common.bridge.entity.MobEntityBridge;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.NearestAttackableTargetGoal;
import net.minecraft.entity.player.ServerPlayerEntity;
import org.bukkit.event.entity.EntityTargetEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NearestAttackableTargetGoal.class)
public class NearestAttackableTargetGoalMixin extends TargetGoalMixin {

    @Shadow protected LivingEntity nearestTarget;

    @Inject(method = "startExecuting", at = @At("HEAD"))
    public void arclight$reason(CallbackInfo ci) {
        ((MobEntityBridge) this.goalOwner).bridge$pushGoalTargetReason(this.nearestTarget instanceof ServerPlayerEntity ? EntityTargetEvent.TargetReason.CLOSEST_PLAYER : EntityTargetEvent.TargetReason.CLOSEST_ENTITY, true);
    }
}
