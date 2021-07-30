package io.izzel.arclight.common.mixin.core.world.entity.animal;

import io.izzel.arclight.common.bridge.core.entity.LivingEntityBridge;
import net.minecraft.world.entity.player.Player;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.world.entity.animal.Dolphin$DolphinSwimWithPlayerGoal")
public class DolphinEntity_SwimWithPlayerGoalMixin {

    // @formatter:off
    @Shadow private Player player;
    // @formatter:on

    @Inject(method = "start", at = @At("HEAD"))
    private void arclight$potionReason1(CallbackInfo ci) {
        ((LivingEntityBridge) this.player).bridge$pushEffectCause(EntityPotionEffectEvent.Cause.DOLPHIN);
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z"))
    private void arclight$potionReason2(CallbackInfo ci) {
        ((LivingEntityBridge) this.player).bridge$pushEffectCause(EntityPotionEffectEvent.Cause.DOLPHIN);
    }
}
