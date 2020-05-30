package io.izzel.arclight.common.mixin.core.item;

import io.izzel.arclight.common.bridge.entity.LivingEntityBridge;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PotionItem;
import net.minecraft.world.World;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PotionItem.class)
public class PotionItemMixin {

    @Inject(method = "onItemUseFinish", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;addPotionEffect(Lnet/minecraft/potion/EffectInstance;)Z"))
    public void arclight$drinkPotion(ItemStack stack, World worldIn, LivingEntity entityLiving, CallbackInfoReturnable<ItemStack> cir) {
        ((LivingEntityBridge) entityLiving).bridge$pushEffectCause(EntityPotionEffectEvent.Cause.POTION_DRINK);
    }
}
