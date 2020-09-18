package io.izzel.arclight.common.mixin.core.entity.passive;

import net.minecraft.entity.passive.ParrotEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ParrotEntity.class)
public abstract class ParrotEntityMixin extends AnimalEntityMixin {

    @Inject(method = "func_230254_b_", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/passive/ParrotEntity;addPotionEffect(Lnet/minecraft/potion/EffectInstance;)Z"))
    private void arclight$feed(PlayerEntity p_230254_1_, Hand p_230254_2_, CallbackInfoReturnable<ActionResultType> cir) {
        bridge$pushEffectCause(EntityPotionEffectEvent.Cause.FOOD);
    }

    @Redirect(method = "attackEntityFrom", at = @At(value = "INVOKE", remap = false, target = "Lnet/minecraft/entity/passive/ParrotEntity;func_233687_w_(Z)V"))
    private void arclight$handledInSuper(ParrotEntity parrotEntity, boolean p_233687_1_) {
    }
}
