package io.izzel.arclight.common.mixin.core.world.item.crafting;

import io.izzel.arclight.common.bridge.core.world.item.crafting.IngredientBridge;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.Nullable;

@Mixin(Ingredient.class)
public abstract class IngredientMixin implements IngredientBridge {

    // @formatter:off
    @Shadow public abstract boolean isEmpty();
    @Shadow public abstract ItemStack[] getItems();
    // @formatter:on

    public boolean exact;

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public boolean test(@Nullable ItemStack itemstack) {
        if (itemstack == null) {
            return false;
        } else if (this.isEmpty()) {
            return itemstack.isEmpty();
        } else {
            ItemStack[] items = this.getItems();
            for (ItemStack stack : items) {
                // CraftBukkit start
                if (exact) {
                    if (ItemStack.isSameItemSameTags(itemstack, stack)) {
                        return true;
                    }
                    continue;
                }
                // CraftBukkit end
                if (stack.is(itemstack.getItem())) {
                    return true;
                }
            }

            return false;
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
