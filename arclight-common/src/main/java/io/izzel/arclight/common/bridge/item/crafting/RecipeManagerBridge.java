package io.izzel.arclight.common.bridge.item.crafting;

import net.minecraft.item.crafting.IRecipe;

public interface RecipeManagerBridge {

    void bridge$addRecipe(IRecipe<?> recipe);

    void bridge$clearRecipes();
}
