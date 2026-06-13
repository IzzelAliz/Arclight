package io.izzel.arclight.common.mod.util;

import io.izzel.arclight.common.bridge.core.world.item.crafting.RecipeManagerBridge;
import io.izzel.arclight.common.mod.server.ArclightServer;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.inventory.CraftComplexRecipe;
import org.bukkit.craftbukkit.util.CraftNamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class ArclightSpecialRecipe extends CraftComplexRecipe {

    private final Recipe<?> recipe;

    public ArclightSpecialRecipe(NamespacedKey id, Recipe<?> recipe) {
        super(id, new ItemStack(Material.AIR), null);
        this.recipe = recipe;
    }

    @Override
    public @NotNull ItemStack getResult() {
        return new ItemStack(Material.AIR);
    }

    @Override
    public void addToCraftingManager() {
        ResourceKey<Recipe<?>> key = ResourceKey.create(Registries.RECIPE, CraftNamespacedKey.toMinecraft(this.getKey()));
        ((RecipeManagerBridge) ArclightServer.getMinecraftServer().getRecipeManager()).bridge$addRecipe(new RecipeHolder<>(key, this.recipe));
    }
}
