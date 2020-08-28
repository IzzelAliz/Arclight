package io.izzel.arclight.common.mixin.core.entity.monster;

import io.izzel.arclight.common.bridge.entity.MobEntityBridge;
import net.minecraft.entity.monster.VexEntity;
import org.bukkit.event.entity.EntityTargetEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.entity.monster.VexEntity.CopyOwnerTargetGoal")
public abstract class VexEntity_CopyOwnerTargetGoalMixin {

    @SuppressWarnings("target") @Shadow(aliases = {"this$0", "field_190883_a"}, remap = false)
    private VexEntity outerThis;

    @Inject(method = "startExecuting", at = @At("HEAD"))
    private void arclight$reason(CallbackInfo ci) {
        ((MobEntityBridge) outerThis).bridge$pushGoalTargetReason(EntityTargetEvent.TargetReason.OWNER_ATTACKED_TARGET, true);
    }
}
