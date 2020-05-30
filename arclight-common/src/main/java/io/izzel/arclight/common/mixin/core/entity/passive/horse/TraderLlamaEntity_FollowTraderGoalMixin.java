package io.izzel.arclight.common.mixin.core.entity.passive.horse;

import io.izzel.arclight.common.bridge.entity.MobEntityBridge;
import io.izzel.arclight.common.mixin.core.entity.ai.goal.TargetGoalMixin;
import net.minecraft.entity.passive.horse.TraderLlamaEntity;
import org.bukkit.event.entity.EntityTargetEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TraderLlamaEntity.FollowTraderGoal.class)
public class TraderLlamaEntity_FollowTraderGoalMixin extends TargetGoalMixin {

    @Inject(method = "startExecuting", at = @At("HEAD"))
    private void arclight$reason(CallbackInfo ci) {
        ((MobEntityBridge) this.goalOwner).bridge$pushGoalTargetReason(EntityTargetEvent.TargetReason.TARGET_ATTACKED_OWNER, true);
    }
}
