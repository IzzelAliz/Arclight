package io.izzel.arclight.common.mixin.core.world.item.crafting;

import io.izzel.arclight.common.bridge.core.item.crafting.IngredientBridge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.Nullable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

@Mixin(Ingredient.class)
public abstract class IngredientMixin implements IngredientBridge {

    // @formatter:off
    @Shadow  public abstract void dissolve();
    @Shadow public ItemStack[] itemStacks;
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
            this.dissolve();
            if (this.itemStacks.length == 0) {
                return stack.isEmpty();
            } else {
                for (ItemStack itemstack : this.itemStacks) {
                    if (exact) {
                        if (itemstack.getItem() == stack.getItem() && ItemStack.isSame(itemstack, stack)) {
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
