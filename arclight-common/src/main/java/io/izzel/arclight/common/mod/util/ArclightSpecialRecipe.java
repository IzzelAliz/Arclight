package io.izzel.arclight.common.mod.util;

import io.izzel.arclight.common.bridge.core.world.item.crafting.RecipeManagerBridge;
import io.izzel.arclight.common.mod.server.ArclightServer;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.v.inventory.CraftComplexRecipe;
import org.bukkit.craftbukkit.v.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v.util.CraftNamespacedKey;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ArclightSpecialRecipe extends CraftComplexRecipe {

    private final Recipe<?> recipe;

    public ArclightSpecialRecipe(NamespacedKey id, Recipe<?> recipe) {
        super(id, new org.bukkit.inventory.ItemStack(Material.AIR), null);
        this.recipe = recipe;
    }

    @Override
    public @NotNull org.bukkit.inventory.ItemStack getResult() {
        ItemStack result = ItemStack.EMPTY;
        List<RecipeDisplay> displays = this.recipe.display();
        if (!displays.isEmpty()) {
            result = displays.getFirst().result().resolveForFirstStack(SlotDisplayContext.fromLevel(ArclightServer.getMinecraftServer().overworld()));
        }
        return CraftItemStack.asCraftMirror(result);
    }

    @Override
    public void addToCraftingManager() {
        ((RecipeManagerBridge) ArclightServer.getMinecraftServer().getRecipeManager()).bridge$addRecipe(new RecipeHolder<>(ResourceKey.create(Registries.RECIPE, CraftNamespacedKey.toMinecraft(this.getKey())), this.recipe));
    }
}
