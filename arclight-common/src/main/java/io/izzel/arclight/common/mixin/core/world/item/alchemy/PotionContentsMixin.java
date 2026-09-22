package io.izzel.arclight.common.mixin.core.world.item.alchemy;

import io.izzel.arclight.common.bridge.core.world.entity.LivingEntityBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.alchemy.PotionContents;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** PotionContents.ConsumableListener is the ordinary potion-drink add-effect path. */
@Mixin(PotionContents.class)
public class PotionContentsMixin {

    @Decorate(method = "*", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;addEffect(Lnet/minecraft/world/effect/MobEffectInstance;)Z"))
    private static boolean arclight$potionContentsEffect(LivingEntity instance, MobEffectInstance effect) throws Throwable {
        ((LivingEntityBridge) instance).bridge$pushEffectCause(EntityPotionEffectEvent.Cause.POTION_DRINK);
        return (boolean) DecorationOps.callsite().invoke(instance, effect);
    }
}
