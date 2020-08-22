package io.izzel.arclight.common.mixin.core.item.crafting;

import io.izzel.arclight.common.bridge.item.crafting.IngredientBridge;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.Nullable;

@Mixin(Ingredient.class)
public abstract class IngredientMixin implements IngredientBridge {

    // @formatter:off
    @Shadow  public abstract void determineMatchingStacks();
    @Shadow public ItemStack[] matchingStacks;
    // @formatter:on

    public boolean exact;

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public boolean test(@Nullable ItemStack stack) {
        if (stack == null) {
            return false;
        } else {
            this.determineMatchingStacks();
            if (this.matchingStacks.length == 0) {
                return stack.isEmpty();
            } else {
                for (ItemStack itemstack : this.matchingStacks) {
                    if (exact) {
                        if (itemstack.getItem() == stack.getItem() && ItemStack.areItemsEqual(itemstack, stack)) {
                            return true;
                        }
                        continue;
                    }
                    if (itemstack.getItem() == stack.getItem()) {
                        return true;
                    }
                }

                return false;
            }
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
