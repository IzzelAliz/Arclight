package io.izzel.arclight.common.mixin.core.world.item.crafting;

import io.izzel.arclight.common.bridge.core.world.item.crafting.IngredientBridge;
import net.minecraft.core.HolderSet;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

@Mixin(Ingredient.class)
public abstract class IngredientMixin implements IngredientBridge {

    @Shadow @Final private HolderSet<Item> values;

    public boolean exact;

    @Inject(method = "test(Lnet/minecraft/world/item/ItemStack;)Z", cancellable = true, at = @At("HEAD"))
    private void arclight$exactMatch(@Nullable ItemStack itemStack, CallbackInfoReturnable<Boolean> cir) {
        if (exact) {
            if (itemStack == null) {
                cir.setReturnValue(false);
                return;
            }
            for (var item : this.values) {
                if (itemStack.is(item) && ItemStack.isSameItemSameComponents(itemStack, item.value().getDefaultInstance())) {
                    cir.setReturnValue(true);
                    return;
                }
            }
            cir.setReturnValue(false);
        }
    }

    @Override
    public void bridge$setExact(boolean exact) {
        this.exact = exact;
    }

    @Override
    public boolean bridge$isExact() {
        return this.exact;
    }
}
