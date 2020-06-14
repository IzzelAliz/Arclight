package io.izzel.arclight.common.mixin.v1_15.entity.item;

import net.minecraft.entity.item.ItemEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ItemEntity.class)
public class ItemEntityMixin_1_15 {

    @Redirect(method = "func_226531_a_", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/item/ItemEntity;setItem(Lnet/minecraft/item/ItemStack;)V"))
    private static void arclight$setNonEmpty(ItemEntity itemEntity, ItemStack stack) {
        if (!stack.isEmpty()) {
            itemEntity.setItem(stack);
        }
    }
}
