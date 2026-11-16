package io.izzel.arclight.common.mixin.core.world.item.crafting;

import io.izzel.arclight.common.bridge.core.world.item.crafting.RecipeManagerBridge;
import io.izzel.arclight.common.mod.server.ArclightServer;
import net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
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
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Mixin(RecipeManager.class)
public abstract class RecipeManagerMixin implements RecipeManagerBridge {

    // @formatter:off
    @Shadow @org.spongepowered.asm.mixin.Final @org.spongepowered.asm.mixin.Mutable private RecipeMap recipes;
    @Shadow @org.spongepowered.asm.mixin.Final @org.spongepowered.asm.mixin.Mutable private java.util.Collection<RecipeHolder<?>> learnableRecipes;
    // @formatter:on

    @Unique private FeatureFlagSet enabledFlags; // Paper added

    @Inject(method = "finalizeRecipeLoading", at = @At("HEAD"))
    private void arclight$captureRecipeFeatures(FeatureFlagSet enabledFlags, CallbackInfo ci) {
        this.enabledFlags = enabledFlags;
    }

    @Unique
    private void arclight$refreshRecipes() {
        if (this.enabledFlags != null) {
            var manager = (RecipeManager) (Object) this;
            manager.finalizeRecipeLoading(this.enabledFlags);
            var packet = new ClientboundUpdateRecipesPacket(manager.getSynchronizedItemProperties(), manager.getSynchronizedStonecutterRecipes());
            for (var player : ArclightServer.getMinecraftServer().getPlayerList().getPlayers()) {
                player.connection.send(packet);
                player.getRecipeBook().sendInitialRecipeBook(player);
                bridge$syncModRecipeContent(player);
            }
        }
    }

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

    @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(
        method = "getRecipeFor(Lnet/minecraft/world/item/crafting/RecipeType;Lnet/minecraft/world/item/crafting/RecipeInput;Lnet/minecraft/world/level/Level;)Ljava/util/Optional;",
        at = @At(value = "INVOKE", target = "Ljava/util/stream/Stream;findFirst()Ljava/util/Optional;"))
    private Optional<RecipeHolder<?>> arclight$preferRuntimeRecipe(java.util.stream.Stream<RecipeHolder<?>> recipes,
            com.llamalad7.mixinextras.injector.wrapoperation.Operation<Optional<RecipeHolder<?>>> original) {
        return recipes.reduce((first, second) -> ((io.izzel.arclight.common.bridge.core.world.item.crafting.RecipeHolderBridge) (Object) second).bridge$isRuntimeAddedRecipe() ? second : first);
    }

    @Unique
    private void arclight$replaceRecipes(List<RecipeHolder<?>> recipes) {
        arclight$publishRecipes(recipes);
        bridge$finalizeRecipeChanges();
    }

    @Unique
    private void arclight$publishRecipes(List<RecipeHolder<?>> recipes) {
        com.google.common.collect.ImmutableMultimap.Builder<RecipeType<?>, RecipeHolder<?>> byType = com.google.common.collect.ImmutableMultimap.builder();
        com.google.common.collect.ImmutableMap.Builder<ResourceKey<Recipe<?>>, RecipeHolder<?>> byKey = com.google.common.collect.ImmutableMap.builder();
        for (RecipeHolder<?> recipe : recipes) {
            byType.put(recipe.value().getType(), recipe);
            byKey.put(recipe.id(), recipe);
        }
        this.recipes = new RecipeMap(byType.build(), byKey.build());
    }

    @Override
    public void bridge$finalizeRecipeChanges() {
        this.learnableRecipes = this.recipes.values().stream().filter(recipe -> !recipe.value().isSpecial()).toList();
        arclight$refreshRecipes();
    }

    @Override
    public void bridge$resetBrewingRecipes(net.minecraft.core.RegistryAccess registryAccess) {
        List<RecipeHolder<?>> recipes = new ArrayList<>(this.recipes.values());
        recipes.removeIf(recipe -> recipe.value().getType() == RecipeType.BREWING);
        registryAccess.lookupOrThrow(net.minecraft.core.registries.Registries.RECIPE).listElements()
            .filter(recipe -> recipe.value().getType() == RecipeType.BREWING)
            .forEach(recipe -> recipes.add(new RecipeHolder<>(recipe.key(), recipe.value())));
        arclight$replaceRecipes(recipes);
    }

    public void addRecipe(RecipeHolder<?> recipe) {
        bridge$addRecipeWithoutRefresh(recipe);
        bridge$finalizeRecipeChanges();
    }

    @Override
    public void bridge$addRecipeWithoutRefresh(RecipeHolder<?> recipe) {
        recipe = ((io.izzel.arclight.common.bridge.core.world.item.crafting.RecipeHolderBridge) (Object) recipe).bridge$asRuntimeAddedRecipe();
        final var key = recipe.id().identifier();
        List<RecipeHolder<?>> list = new ArrayList<>(this.recipes.values());
        if (list.stream().anyMatch(holder -> holder.id().identifier().equals(key))) {
            throw new IllegalStateException("Duplicate recipe ignored with ID " + recipe.id());
        } else {
            list.add(recipe);
            arclight$publishRecipes(list);
        }
    }

    @Override
    public void bridge$addRecipe(RecipeHolder<?> recipe) {
        addRecipe(recipe);
    }

    public boolean removeRecipe(ResourceKey<Recipe<?>> mcKey) {
        return removeRecipe(mcKey.identifier());
    }

    public boolean removeRecipe(Identifier mcKey) {
        boolean removed = arclight$removeRecipeWithoutRefresh(mcKey);
        if (removed) {
            bridge$finalizeRecipeChanges();
        }
        return removed;
    }

    @Override
    public boolean bridge$removeRecipeWithoutRefresh(ResourceKey<Recipe<?>> key) {
        return arclight$removeRecipeWithoutRefresh(key.identifier());
    }

    @Unique
    private boolean arclight$removeRecipeWithoutRefresh(Identifier mcKey) {
        List<RecipeHolder<?>> list = new ArrayList<>(this.recipes.values());
        boolean removed = list.removeIf(recipe -> recipe.id().identifier().equals(mcKey));
        if (removed) {
            arclight$publishRecipes(list);
        }
        return removed;
    }

    public void clearRecipes() {
        arclight$replaceRecipes(List.of());
    }

    @Override
    public void bridge$clearRecipes() {
        clearRecipes();
    }
}
