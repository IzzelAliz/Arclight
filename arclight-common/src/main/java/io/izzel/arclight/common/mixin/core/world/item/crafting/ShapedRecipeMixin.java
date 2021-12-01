package io.izzel.arclight.common.mixin.core.world.item.crafting;

import io.izzel.arclight.common.bridge.core.item.crafting.IRecipeBridge;
import io.izzel.arclight.common.mod.util.ArclightSpecialRecipe;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapedRecipe;
import org.bukkit.craftbukkit.v.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v.inventory.CraftRecipe;
import org.bukkit.craftbukkit.v.inventory.CraftShapedRecipe;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ShapedRecipe.class)
public abstract class ShapedRecipeMixin implements IRecipeBridge {

    // @formatter:off
    @Shadow @Final ItemStack result;
    @Shadow @Final String group;
    @Shadow @Final NonNullList<Ingredient> recipeItems;
    @Shadow public abstract int getHeight();
    @Shadow public abstract int getWidth();
    // @formatter:on

    @Override
    public Recipe bridge$toBukkitRecipe() {
        if (this.getWidth() < 0 || this.getWidth() > 3 || this.getHeight() < 0 || this.getHeight() > 3) {
            return new ArclightSpecialRecipe((net.minecraft.world.item.crafting.Recipe<?>) this);
        }
        CraftItemStack result = CraftItemStack.asCraftMirror(this.result);
        CraftShapedRecipe recipe = new CraftShapedRecipe(result, (ShapedRecipe) (Object) this);
        recipe.setGroup(this.group);

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
        for (Ingredient list : this.recipeItems) {
            RecipeChoice choice = CraftRecipe.toBukkit(list);
            if (choice != null) {
                recipe.setIngredient(c, choice);
            }

            c++;
        }
        return recipe;
    }
}
