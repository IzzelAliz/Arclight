package io.izzel.arclight.common.mixin.core.world.entity.monster;

import io.izzel.arclight.common.bridge.core.entity.MobEntityBridge;
import net.minecraft.world.entity.monster.Vex;
import org.bukkit.event.entity.EntityTargetEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.world.entity.monster.Vex$VexCopyOwnerTargetGoal")
public abstract class Vex_CopyOwnerTargetGoalMixin {

    @SuppressWarnings("target") @Shadow(aliases = {"this$0", "f_34052_"}, remap = false)
    private Vex outerThis;

    @Inject(method = "start", at = @At("HEAD"))
    private void arclight$reason(CallbackInfo ci) {
        ((MobEntityBridge) outerThis).bridge$pushGoalTargetReason(EntityTargetEvent.TargetReason.OWNER_ATTACKED_TARGET, true);
    }
}
