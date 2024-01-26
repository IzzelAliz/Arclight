package io.izzel.arclight.common.mixin.core.world.item.crafting;

import io.izzel.arclight.common.bridge.core.world.item.crafting.RecipeBridge;
import io.izzel.arclight.common.mod.util.ArclightSpecialRecipe;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapedRecipe;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.v.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v.inventory.CraftRecipe;
import org.bukkit.craftbukkit.v.inventory.CraftShapedRecipe;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ShapedRecipe.class)
public abstract class ShapedRecipeMixin implements RecipeBridge {

    // @formatter:off
    @Shadow @Final ItemStack result;
    @Shadow @Final String group;
    @Shadow public abstract int getHeight();
    @Shadow public abstract int getWidth();
    @Shadow public abstract CraftingBookCategory category();
    @Shadow public abstract NonNullList<Ingredient> getIngredients();
    // @formatter:on

    @Override
    public Recipe bridge$toBukkitRecipe(NamespacedKey id) {
        if (this.getWidth() < 1 || this.getWidth() > 3 || this.getHeight() < 1 || this.getHeight() > 3 || this.result.isEmpty()) {
            return new ArclightSpecialRecipe(id, (net.minecraft.world.item.crafting.Recipe<?>) this);
        }
        CraftItemStack result = CraftItemStack.asCraftMirror(this.result);
        CraftShapedRecipe recipe = new CraftShapedRecipe(id, result, (ShapedRecipe) (Object) this);
        recipe.setGroup(this.group);
        recipe.setCategory(CraftRecipe.getCategory(this.category()));

        switch (this.getHeight()) {
            case 1:
                switch (this.getWidth()) {
                    case 1:
                        recipe.shape("a");
                        break;
                    case 2:
                        recipe.shape("ab");
                        break;
                    case 3:
                        recipe.shape("abc");
                        break;
                }
                break;
            case 2:
                switch (this.getWidth()) {
                    case 1:
                        recipe.shape("a", "b");
                        break;
                    case 2:
                        recipe.shape("ab", "cd");
                        break;
                    case 3:
                        recipe.shape("abc", "def");
                        break;
                }
                break;
            case 3:
                switch (this.getWidth()) {
                    case 1:
                        recipe.shape("a", "b", "c");
                        break;
                    case 2:
                        recipe.shape("ab", "cd", "ef");
                        break;
                    case 3:
                        recipe.shape("abc", "def", "ghi");
                        break;
                }
                break;
        }
        char c = 'a';
        for (Ingredient list : this.getIngredients()) {
            RecipeChoice choice = CraftRecipe.toBukkit(list);
            if (choice != null) {
                recipe.setIngredient(c, choice);
            }

            c++;
        }
        return recipe;
    }
}
