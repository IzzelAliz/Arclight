package io.izzel.arclight.neoforge.mixin.core.world.entity.monster.piglin;

import io.izzel.arclight.common.bridge.core.world.entity.monster.piglin.PiglinBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PiglinAi.class)
public abstract class PiglinAiMixin_NeoForge {

    private static boolean customBarterItem(ItemStack stack, Piglin piglin) {
        return ((PiglinBridge) piglin).bridge$getAllowedBarterItems().contains(stack.getItem());
    }

    @Decorate(method = "stopHoldingOffHandItem", at = @At(value = "INVOKE", remap = false, target = "Lnet/minecraft/world/item/ItemStack;isPiglinCurrency()Z"))
    private static boolean arclight$customBarter(ItemStack stack, ServerLevel level, Piglin piglin) throws Throwable {
        return (boolean) DecorationOps.callsite().invoke(stack) || customBarterItem(stack, piglin);
    }

    @Decorate(method = "wantsToPickup", at = @At(value = "INVOKE", remap = false, target = "Lnet/minecraft/world/item/ItemStack;isPiglinCurrency()Z"))
    private static boolean arclight$customBanter2(ItemStack stack, Piglin piglin) throws Throwable {
        return (boolean) DecorationOps.callsite().invoke(stack) || customBarterItem(stack, piglin);
    }

    @Decorate(method = "canAdmire", at = @At(value = "INVOKE", remap = false, target = "Lnet/minecraft/world/item/ItemStack;isPiglinCurrency()Z"))
    private static boolean arclight$customBanter3(ItemStack stack, Piglin piglin) throws Throwable {
        return (boolean) DecorationOps.callsite().invoke(stack) || customBarterItem(stack, piglin);
    }
}
