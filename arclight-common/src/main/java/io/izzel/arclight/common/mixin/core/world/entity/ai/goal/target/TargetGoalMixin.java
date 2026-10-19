package io.izzel.arclight.common.mixin.core.world.entity.ai.goal.target;

import io.izzel.arclight.common.bridge.core.world.entity.MobBridge;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
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

    @Shadow @Final protected Mob mob;

    @io.izzel.arclight.mixin.Decorate(method = "canContinueToUse()Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Mob;setTarget(Lnet/minecraft/world/entity/LivingEntity;)V"))
    private void arclight$continueTarget(Mob mob, net.minecraft.world.entity.LivingEntity target) throws Throwable {
        if (mob.level() != target.level()) {
            io.izzel.arclight.mixin.DecorationOps.cancel().invoke(false);
            return;
        }
        ((MobBridge) mob).bridge$pushGoalTargetReason(EntityTargetEvent.TargetReason.CLOSEST_ENTITY, true);
        io.izzel.arclight.mixin.DecorationOps.callsite().invoke(mob, target);
    }

    @Inject(method = "stop", at = @At("HEAD"))
    private void arclight$reason(CallbackInfo ci) {
        ((MobBridge) this.mob).bridge$pushGoalTargetReason(EntityTargetEvent.TargetReason.FORGOT_TARGET, true);
    }
}
