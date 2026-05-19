package io.izzel.arclight.common.mixin.core.world.food;

import io.izzel.arclight.common.bridge.core.world.food.FoodDataBridge;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FoodProperties.class)
public class FoodPropertiesMixin {

    @Inject(method = "onConsume", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/food/FoodData;eat(Lnet/minecraft/world/food/FoodProperties;)V"))
    private void arclight$eatStack(Level level, LivingEntity livingEntity, ItemStack itemStack, Consumable consumable, CallbackInfo ci) {
        if (livingEntity instanceof Player player) {
            FoodData foodData = player.getFoodData();
            ((FoodDataBridge) foodData).bridge$pushEatStack(itemStack);
        }
    }
}
