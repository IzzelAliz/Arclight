package io.izzel.arclight.common.mixin.core.world.item.crafting;

import io.izzel.arclight.common.bridge.core.world.item.crafting.RecipeManagerBridge;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Mixin(RecipeManager.class)
public abstract class RecipeManagerMixin implements RecipeManagerBridge {

    // @formatter:off
    @Shadow private RecipeMap recipes;
    // @formatter:on

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public <I extends RecipeInput, T extends Recipe<I>> Optional<RecipeHolder<T>> getRecipeFor(RecipeType<T> recipes, I i0, Level world, @Nullable RecipeHolder<T> recipeholder) {
        // CraftBukkit start
        List<RecipeHolder<T>> list = this.recipes.byType(recipes).stream().filter((recipeholder1) -> {
            return recipeholder1.value().matches(i0, world);
        }).toList();
        Optional<RecipeHolder<T>> recipe = (list.isEmpty() || i0.isEmpty()) ? Optional.empty() : (recipeholder != null && recipeholder.value().matches(i0, world) ? Optional.of(recipeholder) : Optional.of(list.getLast())); // CraftBukkit - SPIGOT-4638: last recipe gets priority
        return recipe;
        // CraftBukkit end
    }

    public void addRecipe(RecipeHolder<?> recipe) {
        List<RecipeHolder<?>> list = new ArrayList<>(this.recipes.values());
        if (list.stream().anyMatch(holder -> holder.id().identifier().equals(recipe.id().identifier()))) {
            throw new IllegalStateException("Duplicate recipe ignored with ID " + recipe.id());
        } else {
            list.add(recipe);
            this.recipes = RecipeMap.create(list);
        }
    }

    @Override
    public void bridge$addRecipe(RecipeHolder<?> recipe) {
        addRecipe(recipe);
    }

    public boolean removeRecipe(Identifier mcKey) {
        List<RecipeHolder<?>> list = new ArrayList<>(this.recipes.values());
        boolean removed = list.removeIf(recipe -> recipe.id().identifier().equals(mcKey));
        if (removed) {
            this.recipes = RecipeMap.create(list);
        }
        return removed;
    }

    public void clearRecipes() {
        this.recipes = RecipeMap.create(List.of());
    }

    @Override
    public void bridge$clearRecipes() {
        clearRecipes();
    }
}
