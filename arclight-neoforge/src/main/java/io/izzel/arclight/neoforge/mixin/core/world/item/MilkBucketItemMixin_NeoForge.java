package io.izzel.arclight.neoforge.mixin.core.world.item;

import io.izzel.arclight.common.bridge.core.entity.LivingEntityBridge;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MilkBucketItem;
import net.minecraft.world.level.Level;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MilkBucketItem.class)
public abstract class MilkBucketItemMixin_NeoForge {

    @Inject(method = "finishUsingItem", at = @At(value = "INVOKE", remap = false, target = "Lnet/minecraft/world/entity/LivingEntity;removeEffectsCuredBy(Lnet/neoforged/neoforge/common/EffectCure;)Z"))
    private void arclight$cureReason(ItemStack stack, Level worldIn, LivingEntity entityLiving, CallbackInfoReturnable<ItemStack> cir) {
        ((LivingEntityBridge) entityLiving).bridge$pushEffectCause(EntityPotionEffectEvent.Cause.MILK);
    }
}
