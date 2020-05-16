package io.izzel.arclight.bridge.item.crafting;

import net.minecraft.item.crafting.IRecipe;

public interface RecipeManagerBridge {

    void bridge$addRecipe(IRecipe<?> recipe);

    void bridge$clearRecipes();
}
