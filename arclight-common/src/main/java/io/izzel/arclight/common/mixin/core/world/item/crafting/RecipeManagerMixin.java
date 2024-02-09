package io.izzel.arclight.common.mixin.core.world.item.crafting;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import io.izzel.arclight.common.bridge.core.inventory.IInventoryBridge;
import io.izzel.arclight.common.bridge.core.world.item.crafting.RecipeManagerBridge;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.Container;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
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
    @Shadow public Map<RecipeType<?>, Map<ResourceLocation, RecipeHolder<?>>> recipes;
    @Shadow private boolean hasErrors;
    @Shadow @Final private static Logger LOGGER;
    @Shadow private Map<ResourceLocation, RecipeHolder<?>> byName;
    @Shadow protected static RecipeHolder<?> fromJson(ResourceLocation p_44046_, JsonObject p_44047_) { return null; }
    @Shadow protected abstract <C extends Container, T extends Recipe<C>> Map<ResourceLocation, RecipeHolder<T>> byType(RecipeType<T> p_44055_);
    // @formatter:on

    @Override
    public RecipeHolder<?> bridge$platform$loadRecipe(ResourceLocation key, JsonElement element) {
        return fromJson(key, GsonHelper.convertToJsonObject(element, "top element"));
    }

    /**
     * @author IzzelAluz
     * @reason
     */
    @Overwrite
    @SuppressWarnings("unchecked")
    protected void apply(Map<ResourceLocation, JsonElement> objectIn, ResourceManager resourceManagerIn, ProfilerFiller profilerIn) {
        this.hasErrors = false;
        Map<RecipeType<?>, Object2ObjectLinkedOpenHashMap<ResourceLocation, RecipeHolder<?>>> map = Maps.newHashMap();

        for (RecipeType<?> type : BuiltInRegistries.RECIPE_TYPE) {
            map.put(type, new Object2ObjectLinkedOpenHashMap<>());
        }

        ImmutableMap.Builder<ResourceLocation, RecipeHolder<?>> builder = ImmutableMap.builder();
        for (Map.Entry<ResourceLocation, JsonElement> entry : objectIn.entrySet()) {
            ResourceLocation resourcelocation = entry.getKey();
            if (resourcelocation.getPath().startsWith("_"))
                continue; //Forge: filter anything beginning with "_" as it's used for metadata.

            try {
                RecipeHolder<?> irecipe = this.bridge$platform$loadRecipe(resourcelocation, entry.getValue());
                if (irecipe == null) {
                    LOGGER.debug("Skipping loading recipe {} as it's conditions were not met", resourcelocation);
                    continue;
                }
                map.computeIfAbsent(irecipe.value().getType(), (recipeType) -> new Object2ObjectLinkedOpenHashMap<>())
                    .putAndMoveToFirst(resourcelocation, irecipe);
                builder.put(resourcelocation, irecipe);
            } catch (IllegalArgumentException | JsonParseException jsonparseexception) {
                LOGGER.error("Parsing error loading recipe {}", resourcelocation, jsonparseexception);
            }
        }

        this.recipes = (Map) map;
        this.byName = Maps.newHashMap(builder.build());
        LOGGER.info("Loaded {} recipes", map.size());
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public <C extends Container, T extends Recipe<C>> Optional<RecipeHolder<T>> getRecipeFor(RecipeType<T> recipeTypeIn, C inventoryIn, Level worldIn) {
        Optional<RecipeHolder<T>> optional = this.byType(recipeTypeIn).values().stream().filter((recipe) -> {
            return recipe.value().matches(inventoryIn, worldIn);
        }).findFirst();
        ((IInventoryBridge) inventoryIn).setCurrentRecipe(optional.orElse(null));
        return optional;
    }

    public void addRecipe(RecipeHolder<?> recipe) {
        if (this.recipes instanceof ImmutableMap) {
            this.recipes = new HashMap<>(recipes);
        }
        if (this.byName instanceof ImmutableMap) {
            this.byName = new HashMap<>(byName);
        }
        Map<ResourceLocation, RecipeHolder<?>> original = this.recipes.get(recipe.value().getType());
        Object2ObjectLinkedOpenHashMap<ResourceLocation, RecipeHolder<?>> map;
        if (!(original instanceof Object2ObjectLinkedOpenHashMap)) {
            Object2ObjectLinkedOpenHashMap<ResourceLocation, RecipeHolder<?>> hashMap = new Object2ObjectLinkedOpenHashMap<>();
            hashMap.putAll(original);
            this.recipes.put(recipe.value().getType(), hashMap);
            map = hashMap;
        } else {
            map = ((Object2ObjectLinkedOpenHashMap<ResourceLocation, RecipeHolder<?>>) original);
        }

        if (this.byName.containsKey(recipe.id()) || map.containsKey(recipe.id())) {
            throw new IllegalStateException("Duplicate recipe ignored with ID " + recipe.id());
        } else {
            map.putAndMoveToFirst(recipe.id(), recipe);
            this.byName.put(recipe.id(), recipe);
        }
    }

    @Override
    public void bridge$addRecipe(RecipeHolder<?> recipe) {
        addRecipe(recipe);
    }

    public boolean removeRecipe(ResourceLocation mcKey) {
        for (var recipes : recipes.values()) {
            recipes.remove(mcKey);
        }
        return byName.remove(mcKey) != null;
    }

    public void clearRecipes() {
        this.recipes = new HashMap<>();
        for (RecipeType<?> type : BuiltInRegistries.RECIPE_TYPE) {
            this.recipes.put(type, new Object2ObjectLinkedOpenHashMap<>());
        }
        this.byName = new HashMap<>();
    }

    @Override
    public void bridge$clearRecipes() {
        clearRecipes();
    }
}
