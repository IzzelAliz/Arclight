package io.izzel.arclight.common.mixin.core.entity.passive;

import net.minecraft.entity.ai.goal.SitGoal;
import net.minecraft.entity.passive.ParrotEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ParrotEntity.class)
public abstract class ParrotEntityMixin extends AnimalEntityMixin {

    @Inject(method = "processInteract", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/passive/ParrotEntity;addPotionEffect(Lnet/minecraft/potion/EffectInstance;)Z"))
    private void arclight$feed(PlayerEntity player, Hand hand, CallbackInfoReturnable<Boolean> cir) {
        bridge$pushEffectCause(EntityPotionEffectEvent.Cause.FOOD);
    }

    @Redirect(method = "attackEntityFrom", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/ai/goal/SitGoal;setSitting(Z)V"))
    private void arclight$handledInSuper(SitGoal sitGoal, boolean sitting) {
    }
}
