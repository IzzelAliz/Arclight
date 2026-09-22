package io.izzel.arclight.common.mixin.core.world.item.crafting;

import io.izzel.arclight.common.bridge.core.world.item.crafting.IngredientBridge;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.util.List;

@Mixin(Ingredient.class)
public abstract class IngredientMixin implements IngredientBridge {

    private List<ItemStack> exactChoices;

    @Inject(method = "test(Lnet/minecraft/world/item/ItemStack;)Z", cancellable = true, at = @At("HEAD"))
    private void arclight$exactMatch(@Nullable ItemStack itemStack, CallbackInfoReturnable<Boolean> cir) {
        if (this.exactChoices != null) {
            if (itemStack == null) {
                cir.setReturnValue(false);
                return;
            }
            for (ItemStack exactChoice : this.exactChoices) {
                if (ItemStack.isSameItemSameComponents(itemStack, exactChoice)) {
                    cir.setReturnValue(true);
                    return;
                }
            }
            cir.setReturnValue(false);
        }
    }

    @Override
    public void bridge$setExactChoices(List<ItemStack> exactChoices) {
        this.exactChoices = exactChoices.stream().map(ItemStack::copy).toList();
    }

    @Override
    public List<ItemStack> bridge$getExactChoices() {
        if (this.exactChoices == null) {
            return null;
        }
        return this.exactChoices.stream().map(ItemStack::copy).toList();
    }

    @Override
    public boolean bridge$isExact() {
        return this.exactChoices != null;
    }
}
