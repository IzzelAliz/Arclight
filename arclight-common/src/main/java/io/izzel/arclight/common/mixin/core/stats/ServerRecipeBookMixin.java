package io.izzel.arclight.common.mixin.core.stats;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.RecipeBook;
import net.minecraft.stats.ServerRecipeBook;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Mixin(ServerRecipeBook.class)
public abstract class ServerRecipeBookMixin extends RecipeBook {

    // @formatter:off
    @Shadow protected Set<ResourceKey<Recipe<?>>> known;
    // @formatter:on

    @ModifyVariable(method = "addRecipes", at = @At("HEAD"), argsOnly = true, index = 0)
    private Collection<RecipeHolder<?>> arclight$filterRecipes(Collection<RecipeHolder<?>> recipes, ServerPlayer player) {
        return recipes.stream().filter(holder -> {
            if (holder.value().isSpecial() || this.known.contains(holder.id())) {
                return true;
            }
            Identifier id = holder.id().identifier();
            return CraftEventFactory.handlePlayerRecipeListUpdateEvent(player, id);
        }).collect(Collectors.toList());
    }
}
