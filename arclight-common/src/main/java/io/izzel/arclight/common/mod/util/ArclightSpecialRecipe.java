package io.izzel.arclight.common.mod.util;

import io.izzel.arclight.common.bridge.core.item.crafting.RecipeManagerBridge;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.v.inventory.CraftComplexRecipe;
import org.bukkit.craftbukkit.v.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v.util.CraftNamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class ArclightSpecialRecipe extends CraftComplexRecipe {

    private final Recipe<?> recipe;

    public ArclightSpecialRecipe(NamespacedKey id, Recipe<?> recipe) {
        super(id, null);
        this.recipe = recipe;
    }

    @Override
    public @NotNull ItemStack getResult() {
        return CraftItemStack.asCraftMirror(this.recipe.getResultItem(ServerLifecycleHooks.getCurrentServer().registryAccess()));
    }

    @Override
    public void addToCraftingManager() {
        ((RecipeManagerBridge) ServerLifecycleHooks.getCurrentServer().getRecipeManager()).bridge$addRecipe(new RecipeHolder<>(CraftNamespacedKey.toMinecraft(this.getKey()), this.recipe));
    }
}
