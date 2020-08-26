package io.izzel.arclight.common.mixin.core.entity.monster;

import net.minecraft.entity.monster.WitchEntity;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WitchEntity.class)
public abstract class WitchEntityMixin extends AbstractRaiderEntityMixin {

    @Inject(method = "livingTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/monster/WitchEntity;addPotionEffect(Lnet/minecraft/potion/EffectInstance;)Z"))
    private void arclight$reason(CallbackInfo ci) {
        bridge$pushEffectCause(EntityPotionEffectEvent.Cause.ATTACK);
    }
}
