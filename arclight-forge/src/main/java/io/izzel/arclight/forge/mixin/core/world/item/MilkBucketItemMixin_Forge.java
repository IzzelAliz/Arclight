package io.izzel.arclight.forge.mixin.core.world.item;

import io.izzel.arclight.common.bridge.core.world.entity.LivingEntityBridge;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.consume_effects.ClearAllStatusEffectsConsumeEffect;
import net.minecraft.world.level.Level;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClearAllStatusEffectsConsumeEffect.class)
public class MilkBucketItemMixin_Forge {

    @Inject(method = "apply(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;removeAllEffects()Z"), require = 1, allow = 1)
    private void arclight$cureReason(Level worldIn, ItemStack stack, LivingEntity entityLiving, CallbackInfoReturnable<Boolean> cir) {
        // This consume effect is reusable; only milk carries the MILK cause.
        if (stack.is(Items.MILK_BUCKET)) {
            ((LivingEntityBridge) entityLiving).bridge$pushEffectCause(EntityPotionEffectEvent.Cause.MILK);
        }
    }
}
