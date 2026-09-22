package io.izzel.arclight.common.mixin.vanilla.world.item;

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
public abstract class MilkBucketItemMixin_Vanilla {
    @Inject(method = "apply", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;removeAllEffects()Z"))
    private void arclight$cureReason(Level worldIn, ItemStack stack, LivingEntity entityLiving, CallbackInfoReturnable<Boolean> cir) {
        // The clear effect is reusable; only a real milk stack carries MILK.
        if (stack.is(Items.MILK_BUCKET)) {
            ((LivingEntityBridge) entityLiving).bridge$pushEffectCause(EntityPotionEffectEvent.Cause.MILK);
        }
    }
}
