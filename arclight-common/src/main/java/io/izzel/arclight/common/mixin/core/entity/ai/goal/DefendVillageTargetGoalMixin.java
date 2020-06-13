package io.izzel.arclight.common.mixin.core.entity.ai.goal;

import io.izzel.arclight.common.bridge.entity.MobEntityBridge;
import net.minecraft.entity.ai.goal.DefendVillageTargetGoal;
import net.minecraft.entity.passive.IronGolemEntity;
import org.bukkit.event.entity.EntityTargetEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DefendVillageTargetGoal.class)
public class DefendVillageTargetGoalMixin {

    @Shadow @Final private IronGolemEntity irongolem;

    @Inject(method = "startExecuting", at = @At("HEAD"))
    public void arclight$reason(CallbackInfo ci) {
        ((MobEntityBridge) this.irongolem).bridge$pushGoalTargetReason(EntityTargetEvent.TargetReason.DEFEND_VILLAGE, true);
    }
}
