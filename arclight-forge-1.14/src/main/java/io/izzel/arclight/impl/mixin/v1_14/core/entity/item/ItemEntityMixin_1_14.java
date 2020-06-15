package io.izzel.arclight.impl.mixin.v1_14.core.entity.item;

import net.minecraft.entity.item.ItemEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ItemEntity.class)
public class ItemEntityMixin_1_14 {

    @Redirect(method = "func_213858_a", at = @At(value = "INVOKE", ordinal = 1, target = "Lnet/minecraft/entity/item/ItemEntity;setItem(Lnet/minecraft/item/ItemStack;)V"))
    private static void arclight$setNonEmpty(ItemEntity itemEntity, ItemStack stack) {
        if (!stack.isEmpty()) {
            itemEntity.setItem(stack);
        }
    }
}
