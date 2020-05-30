package io.izzel.arclight.common.mixin.core.entity.passive.fish;

import io.izzel.arclight.common.bridge.entity.LivingEntityBridge;
import io.izzel.arclight.common.bridge.entity.MobEntityBridge;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.passive.fish.PufferfishEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PufferfishEntity.class)
public abstract class PufferfishEntityMixin extends AbstractFishEntityMixin {

    @Inject(method = "attack", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/MobEntity;addPotionEffect(Lnet/minecraft/potion/EffectInstance;)Z"))
    private void arclight$attack(MobEntity mobEntity, CallbackInfo ci) {
        ((MobEntityBridge) mobEntity).bridge$pushEffectCause(EntityPotionEffectEvent.Cause.ATTACK);
    }

    @Inject(method = "onCollideWithPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;addPotionEffect(Lnet/minecraft/potion/EffectInstance;)Z"))
    private void arclight$collide(PlayerEntity entityIn, CallbackInfo ci) {
        ((LivingEntityBridge) entityIn).bridge$pushEffectCause(EntityPotionEffectEvent.Cause.ATTACK);
    }
}
