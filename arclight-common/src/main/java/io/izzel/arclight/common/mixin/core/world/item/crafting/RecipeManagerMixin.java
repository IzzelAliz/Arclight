package io.izzel.arclight.common.mixin.core.world.item.crafting;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.gson.JsonElement;
import io.izzel.arclight.common.bridge.core.world.item.crafting.RecipeManagerBridge;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Mixin(RecipeManager.class)
public abstract class RecipeManagerMixin implements RecipeManagerBridge {

    // @formatter:off
    @Shadow private boolean hasErrors;
    @Shadow @Final private static Logger LOGGER;
    @Shadow private Map<ResourceLocation, RecipeHolder<?>> byName;
    @Shadow public Multimap<RecipeType<?>, RecipeHolder<?>> byType;
    @Shadow protected abstract <I extends RecipeInput, T extends Recipe<I>> Collection<RecipeHolder<T>> byType(RecipeType<T> recipeType);
    // @formatter:on

    @Inject(method = "apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V", at = @At("RETURN"))
    private void arclight$makeMutable(Map<ResourceLocation, JsonElement> map, ResourceManager resourceManager, ProfilerFiller profilerFiller, CallbackInfo ci) {
        this.byName = new HashMap<>(this.byName);
        this.byType = LinkedHashMultimap.create(this.byType);
    }

    @Inject(method = "replaceRecipes", at = @At("RETURN"))
    private void arclight$replaceMutable(Iterable<RecipeHolder<?>> iterable, CallbackInfo ci) {
        this.byName = new HashMap<>(this.byName);
        this.byType = LinkedHashMultimap.create(this.byType);
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public <I extends RecipeInput, T extends Recipe<I>> Optional<RecipeHolder<T>> getRecipeFor(RecipeType<T> recipes, I i0, Level world, @Nullable RecipeHolder<T> recipeholder) {
        // CraftBukkit start
        List<RecipeHolder<T>> list = this.byType(recipes).stream().filter((recipeholder1) -> {
            return recipeholder1.value().matches(i0, world);
        }).toList();
        Optional<RecipeHolder<T>> recipe = (list.isEmpty() || i0.isEmpty()) ? Optional.empty() : (recipeholder != null && recipeholder.value().matches(i0, world) ? Optional.of(recipeholder) : Optional.of(list.getLast())); // CraftBukkit - SPIGOT-4638: last recipe gets priority
        return recipe;
        // CraftBukkit end
    }

    public void addRecipe(RecipeHolder<?> recipe) {
        if (this.byType instanceof ImmutableMultimap<RecipeType<?>, RecipeHolder<?>>) {
            this.byType = LinkedHashMultimap.create(this.byType);
        }
        if (this.byName instanceof ImmutableMap) {
            this.byName = new HashMap<>(byName);
        }
        Collection<RecipeHolder<?>> map = this.byType.get(recipe.value().getType());

        if (this.byName.containsKey(recipe.id())) {
            throw new IllegalStateException("Duplicate recipe ignored with ID " + recipe.id());
        } else {
            map.add(recipe);
            this.byName.put(recipe.id(), recipe);
        }
    }

    @Override
    public void bridge$addRecipe(RecipeHolder<?> recipe) {
        addRecipe(recipe);
    }

    public boolean removeRecipe(ResourceLocation mcKey) {
        byType.values().removeIf(recipe -> recipe.id().equals(mcKey));
        return byName.remove(mcKey) != null;
    }

    public void clearRecipes() {
        this.byType = LinkedHashMultimap.create();
        this.byName = Maps.newHashMap();
    }

    @Override
    public void bridge$clearRecipes() {
        clearRecipes();
    }
}
