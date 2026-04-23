package io.izzel.arclight.common.mixin.core.world.entity.ai.goal.target;

import io.izzel.arclight.common.bridge.core.world.entity.MobBridge;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import org.bukkit.event.entity.EntityTargetEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(OwnerHurtTargetGoal.class)
public class OwnerHurtTargetGoalMixin extends TargetGoalMixin {

    @Inject(method = "start", at = @At("HEAD"))
    public void arclight$reason(CallbackInfo ci) {
        ((MobBridge) this.mob).bridge$pushGoalTargetReason(EntityTargetEvent.TargetReason.OWNER_ATTACKED_TARGET, true);
    }
}
