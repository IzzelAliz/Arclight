package io.izzel.arclight.common.mixin.core.item.crafting;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import io.izzel.arclight.common.bridge.inventory.IInventoryBridge;
import io.izzel.arclight.common.bridge.item.crafting.RecipeManagerBridge;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.item.crafting.RecipeManager;
import net.minecraft.profiler.IProfiler;
import net.minecraft.resources.IResourceManager;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Util;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import net.minecraftforge.common.crafting.CraftingHelper;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Mixin(RecipeManager.class)
public abstract class RecipeManagerMixin implements RecipeManagerBridge {

    // @formatter:off
    @Shadow public Map<IRecipeType<?>, Map<ResourceLocation, IRecipe<?>>> recipes;
    @Shadow protected abstract <C extends IInventory, T extends IRecipe<C>> Map<ResourceLocation, IRecipe<C>> getRecipes(IRecipeType<T> recipeTypeIn);
    @Shadow private boolean someRecipesErrored;
    @Shadow @Final private static Logger LOGGER;
    @Shadow public static IRecipe<?> deserializeRecipe(ResourceLocation recipeId, JsonObject json) { return null; }
    // @formatter:on

    /**
     * @author IzzelAluz
     * @reason
     */
    @Overwrite
    @SuppressWarnings("unchecked")
    protected void apply(Map<ResourceLocation, JsonElement> objectIn, IResourceManager resourceManagerIn, IProfiler profilerIn) {
        this.someRecipesErrored = false;
        Map<IRecipeType<?>, Object2ObjectLinkedOpenHashMap<ResourceLocation, IRecipe<?>>> map = Maps.newHashMap();

        for (IRecipeType<?> type : Registry.RECIPE_TYPE) {
            map.put(type, new Object2ObjectLinkedOpenHashMap<>());
        }

        for (Map.Entry<ResourceLocation, JsonElement> entry : objectIn.entrySet()) {
            ResourceLocation resourcelocation = entry.getKey();
            if (resourcelocation.getPath().startsWith("_"))
                continue; //Forge: filter anything beginning with "_" as it's used for metadata.

            try {
                if (entry.getValue().isJsonObject() && !CraftingHelper.processConditions(entry.getValue().getAsJsonObject(), "conditions")) {
                    LOGGER.info("Skipping loading recipe {} as it's conditions were not met", resourcelocation);
                    continue;
                }
                IRecipe<?> irecipe = deserializeRecipe(resourcelocation, JSONUtils.getJsonObject(entry.getValue(), "top element"));
                if (irecipe == null) {
                    LOGGER.info("Skipping loading recipe {} as it's serializer returned null", resourcelocation);
                    continue;
                }
                map.computeIfAbsent(irecipe.getType(), (recipeType) -> new Object2ObjectLinkedOpenHashMap<>())
                    .putAndMoveToFirst(resourcelocation, irecipe);
            } catch (IllegalArgumentException | JsonParseException jsonparseexception) {
                LOGGER.error("Parsing error loading recipe {}", resourcelocation, jsonparseexception);
            }
        }

        this.recipes = (Map) map;
        LOGGER.info("Loaded {} recipes", map.size());
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public <C extends IInventory, T extends IRecipe<C>> Optional<T> getRecipe(IRecipeType<T> recipeTypeIn, C inventoryIn, World worldIn) {
        Optional<T> optional = this.getRecipes(recipeTypeIn).values().stream().flatMap((recipe) -> {
            return Util.streamOptional(recipeTypeIn.matches(recipe, worldIn, inventoryIn));
        }).findFirst();
        ((IInventoryBridge) inventoryIn).setCurrentRecipe(optional.orElse(null));
        return optional;
    }

    public void addRecipe(IRecipe<?> recipe) {
        if (this.recipes instanceof ImmutableMap) {
            this.recipes = new HashMap<>(recipes);
        }
        Map<ResourceLocation, IRecipe<?>> original = this.recipes.get(recipe.getType());
        Object2ObjectLinkedOpenHashMap<ResourceLocation, IRecipe<?>> map;
        if (!(original instanceof Object2ObjectLinkedOpenHashMap)) {
            Object2ObjectLinkedOpenHashMap<ResourceLocation, IRecipe<?>> hashMap = new Object2ObjectLinkedOpenHashMap<>();
            hashMap.putAll(original);
            this.recipes.put(recipe.getType(), hashMap);
            map = hashMap;
        } else {
            map = ((Object2ObjectLinkedOpenHashMap<ResourceLocation, IRecipe<?>>) original);
        }

        if (map.containsKey(recipe.getId())) {
            throw new IllegalStateException("Duplicate recipe ignored with ID " + recipe.getId());
        } else {
            map.putAndMoveToFirst(recipe.getId(), recipe);
        }
    }

    @Override
    public void bridge$addRecipe(IRecipe<?> recipe) {
        addRecipe(recipe);
    }

    public void clearRecipes() {
        this.recipes = new HashMap<>();
        for (IRecipeType<?> type : Registry.RECIPE_TYPE) {
            this.recipes.put(type, new Object2ObjectLinkedOpenHashMap<>());
        }
    }

    @Override
    public void bridge$clearRecipes() {
        clearRecipes();
    }
}
