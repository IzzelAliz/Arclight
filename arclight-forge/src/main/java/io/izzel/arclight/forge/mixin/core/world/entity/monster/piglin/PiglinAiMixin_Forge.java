package io.izzel.arclight.forge.mixin.core.world.entity.monster.piglin;

import io.izzel.arclight.common.bridge.core.entity.monster.piglin.PiglinBridge;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PiglinAi.class)
public abstract class PiglinAiMixin_Forge {

    // @formatter:off
    @Shadow private static boolean isBarterCurrency(ItemStack arg) { return false; }
    // @formatter:on

    @Redirect(method = "stopHoldingOffHandItem", at = @At(value = "INVOKE", remap = false, target = "Lnet/minecraft/world/item/ItemStack;isPiglinCurrency()Z"))
    private static boolean arclight$customBarter(ItemStack stack, Piglin piglin) {
        return isBarterCurrency(stack) || ((PiglinBridge) piglin).bridge$getAllowedBarterItems().contains(stack.getItem());
    }

    @Redirect(method = "wantsToPickup", at = @At(value = "INVOKE", remap = false, target = "Lnet/minecraft/world/item/ItemStack;isPiglinCurrency()Z"))
    private static boolean arclight$customBanter2(ItemStack stack, Piglin piglin) {
        return isBarterCurrency(stack) || ((PiglinBridge) piglin).bridge$getAllowedBarterItems().contains(stack.getItem());
    }

    @Redirect(method = "canAdmire", at = @At(value = "INVOKE", remap = false, target = "Lnet/minecraft/world/item/ItemStack;isPiglinCurrency()Z"))
    private static boolean arclight$customBanter3(ItemStack stack, Piglin piglin) {
        return isBarterCurrency(stack) || ((PiglinBridge) piglin).bridge$getAllowedBarterItems().contains(stack.getItem());
    }
}
