package io.izzel.arclight.common.mod.util;

import io.izzel.arclight.common.bridge.core.item.crafting.RecipeManagerBridge;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.v.inventory.CraftComplexRecipe;
import org.bukkit.craftbukkit.v.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v.util.CraftNamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class ArclightSpecialRecipe extends CraftComplexRecipe {

    private final Recipe<?> recipe;

    public ArclightSpecialRecipe(Recipe<?> recipe) {
        super(null);
        this.recipe = recipe;
    }

    @Override
    public @NotNull ItemStack getResult() {
        return CraftItemStack.asCraftMirror(this.recipe.getResultItem());
    }

    @Override
    public @NotNull NamespacedKey getKey() {
        return CraftNamespacedKey.fromMinecraft(this.recipe.getId());
    }

    @Override
    public void addToCraftingManager() {
        ((RecipeManagerBridge) ServerLifecycleHooks.getCurrentServer().getRecipeManager()).bridge$addRecipe(this.recipe);
    }
}
