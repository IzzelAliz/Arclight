package io.izzel.arclight.common.mod.util;

import io.izzel.arclight.common.bridge.item.crafting.RecipeManagerBridge;
import net.minecraft.item.crafting.IRecipe;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.v.inventory.CraftComplexRecipe;
import org.bukkit.craftbukkit.v.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v.util.CraftNamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class ArclightSpecialRecipe extends CraftComplexRecipe {

    private final IRecipe<?> recipe;

    public ArclightSpecialRecipe(IRecipe<?> recipe) {
        super(null);
        this.recipe = recipe;
    }

    @Override
    public @NotNull ItemStack getResult() {
        return CraftItemStack.asCraftMirror(this.recipe.getRecipeOutput());
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
