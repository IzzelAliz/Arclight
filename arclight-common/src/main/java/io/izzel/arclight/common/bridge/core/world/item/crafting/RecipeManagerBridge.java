package io.izzel.arclight.common.bridge.core.world.item.crafting;

import com.google.gson.JsonElement;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeHolder;

public interface RecipeManagerBridge {

    void bridge$addRecipe(RecipeHolder<?> recipe);

    void bridge$clearRecipes();

    boolean removeRecipe(net.minecraft.resources.ResourceKey<net.minecraft.world.item.crafting.Recipe<?>> key);

    default void bridge$syncModRecipeContent(net.minecraft.server.level.ServerPlayer player) {
    }

    default void addRecipe(net.minecraft.world.item.crafting.RecipeHolder<?> recipe) {
        this.bridge$addRecipe(recipe);
    }

    default void clearRecipes() {
        this.bridge$clearRecipes();
    }

}
