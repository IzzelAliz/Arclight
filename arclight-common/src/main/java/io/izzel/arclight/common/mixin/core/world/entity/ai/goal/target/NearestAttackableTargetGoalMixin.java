package io.izzel.arclight.common.mixin.core.world.entity.ai.goal.target;

import io.izzel.arclight.common.bridge.core.world.entity.MobBridge;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import org.bukkit.event.entity.EntityTargetEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NearestAttackableTargetGoal.class)
public class NearestAttackableTargetGoalMixin extends TargetGoalMixin {

    @Shadow protected LivingEntity target;

    @Inject(method = "start", at = @At("HEAD"))
    public void arclight$reason(CallbackInfo ci) {
        ((MobBridge) this.mob).bridge$pushGoalTargetReason(this.target instanceof ServerPlayer ? EntityTargetEvent.TargetReason.CLOSEST_PLAYER : EntityTargetEvent.TargetReason.CLOSEST_ENTITY, true);
    }
}
