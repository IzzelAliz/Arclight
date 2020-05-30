package io.izzel.arclight.common.mixin.core.entity.passive;

import io.izzel.arclight.common.bridge.entity.LivingEntityBridge;
import net.minecraft.entity.player.PlayerEntity;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.entity.passive.DolphinEntity.SwimWithPlayerGoal")
public class DolphinEntity_SwimWithPlayerGoalMixin {

    // @formatter:off
    @Shadow private PlayerEntity targetPlayer;
    // @formatter:on

    @Inject(method = "startExecuting", at = @At("HEAD"))
    private void arclight$potionReason1(CallbackInfo ci) {
        ((LivingEntityBridge) this.targetPlayer).bridge$pushEffectCause(EntityPotionEffectEvent.Cause.DOLPHIN);
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;addPotionEffect(Lnet/minecraft/potion/EffectInstance;)Z"))
    private void arclight$potionReason2(CallbackInfo ci) {
        ((LivingEntityBridge) this.targetPlayer).bridge$pushEffectCause(EntityPotionEffectEvent.Cause.DOLPHIN);
    }
}
