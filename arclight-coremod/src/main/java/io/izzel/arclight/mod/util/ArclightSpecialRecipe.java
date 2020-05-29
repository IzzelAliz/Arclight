package io.izzel.arclight.mod.util;

import io.izzel.arclight.bridge.item.crafting.RecipeManagerBridge;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.SpecialRecipe;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.CraftServer;
import org.bukkit.craftbukkit.v.inventory.CraftRecipe;
import org.bukkit.craftbukkit.v.inventory.CraftShapedRecipe;
import org.bukkit.craftbukkit.v.inventory.CraftShapelessRecipe;
import org.bukkit.craftbukkit.v.util.CraftNamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;

public class ArclightSpecialRecipe {

    public static class Shapeless extends CraftShapelessRecipe {

        private final IRecipe<?> recipe;

        public Shapeless(ItemStack result, IRecipe<?> recipe) {
            super(CraftNamespacedKey.fromMinecraft(recipe.getId()), result);
            this.recipe = recipe;
            this.setGroup(this.recipe.getGroup());
        }

        @Override
        public void addToCraftingManager() {
            ((RecipeManagerBridge) ((CraftServer) Bukkit.getServer()).getServer()
                .getRecipeManager()).bridge$addRecipe(recipe);
        }
    }

    public static class Shaped extends CraftShapedRecipe {

        private final SpecialRecipe recipe;

        public Shaped(ItemStack result, SpecialRecipe recipe) {
            super(CraftNamespacedKey.fromMinecraft(recipe.getId()), result);
            this.recipe = recipe;
            this.setGroup(this.recipe.getGroup());
        }

        @Override
        public void addToCraftingManager() {
            ((RecipeManagerBridge) ((CraftServer) Bukkit.getServer()).getServer()
                .getRecipeManager()).bridge$addRecipe(recipe);
        }
    }

    public static CraftShapelessRecipe shapeless(ItemStack result, IRecipe<?> recipe, Ingredient... ingredients) {
        Shapeless shapeless = new Shapeless(result, recipe);
        for (Ingredient ingredient : ingredients) {
            shapeless.addIngredient(CraftRecipe.toBukkit(ingredient));
        }
        return shapeless;
    }

    public static CraftShapedRecipe shaped(ItemStack result, SpecialRecipe recipe, int width, Ingredient... ingredients) {
        Shaped shaped = new Shaped(result, recipe);
        int height = ingredients.length / width;
        String[] shape = new String[height];
        char c = 'a';
        for (int i = 0; i < height; i++) {
            StringBuilder builder = new StringBuilder();
            for (int j = 0; j < width; j++) {
                builder.append(c++);
            }
            shape[i] = builder.toString();
        }
        shaped.shape(shape);
        c = 'a';
        for (Ingredient ingredient : ingredients) {
            RecipeChoice choice = CraftRecipe.toBukkit(ingredient);
            if (choice != null) {
                shaped.setIngredient(c, choice);
            }
            c++;
        }
        return shaped;
    }
}
