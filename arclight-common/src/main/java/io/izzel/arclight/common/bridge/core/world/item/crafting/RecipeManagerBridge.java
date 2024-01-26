package io.izzel.arclight.common.bridge.core.world.item.crafting;

import com.google.gson.JsonElement;
import net.minecraft.world.item.crafting.RecipeHolder;

public interface RecipeManagerBridge {

    void bridge$addRecipe(RecipeHolder<?> recipe);

    void bridge$clearRecipes();

    default boolean bridge$forge$conditionNotMet(JsonElement element) {
        return false;
    }
}
