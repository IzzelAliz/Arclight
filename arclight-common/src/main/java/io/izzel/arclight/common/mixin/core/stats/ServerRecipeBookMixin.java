package io.izzel.arclight.common.mixin.core.stats;

import com.google.common.collect.Lists;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.network.protocol.game.ClientboundRecipeBookAddPacket;
import net.minecraft.network.protocol.game.ClientboundRecipeBookRemovePacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.RecipeBook;
import net.minecraft.stats.ServerRecipeBook;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

@Mixin(ServerRecipeBook.class)
public abstract class ServerRecipeBookMixin extends RecipeBook {

    // @formatter:off
    @Shadow @Final public Set<ResourceKey<Recipe<?>>> known;
    @Shadow @Final private ServerRecipeBook.DisplayResolver displayResolver;
    @Shadow protected abstract void addHighlight(ResourceKey<Recipe<?>> id);
    @Shadow public abstract void add(ResourceKey<Recipe<?>> id);
    @Shadow public abstract void remove(ResourceKey<Recipe<?>> id);
    // @formatter:on

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public int addRecipes(Collection<RecipeHolder<?>> recipes, ServerPlayer player) {
        List<ClientboundRecipeBookAddPacket.Entry> list = new ArrayList<>();

        for (RecipeHolder<?> recipeholder : recipes) {
            ResourceKey<Recipe<?>> resourcekey = recipeholder.id();
            if (!this.known.contains(resourcekey) && !recipeholder.value().isSpecial() && CraftEventFactory.handlePlayerRecipeListUpdateEvent(player, resourcekey.identifier())) {
                this.add(resourcekey);
                this.addHighlight(resourcekey);
                this.displayResolver.displaysForRecipe(resourcekey, (recipedisplayentry) -> {
                    list.add(new ClientboundRecipeBookAddPacket.Entry(recipedisplayentry, recipeholder.value().showNotification(), true));
                });
                CriteriaTriggers.RECIPE_UNLOCKED.trigger(player, recipeholder);
            }
        }

        if (!list.isEmpty() && player.connection != null) {
            player.connection.send(new ClientboundRecipeBookAddPacket(list, false));
        }

        return list.size();
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public int removeRecipes(Collection<RecipeHolder<?>> recipes, ServerPlayer player) {
        List<RecipeDisplayId> list = Lists.newArrayList();

        for (RecipeHolder<?> recipeholder : recipes) {
            ResourceKey<Recipe<?>> resourcekey = recipeholder.id();

            if (this.known.contains(resourcekey)) {
                this.remove(resourcekey);
                this.displayResolver.displaysForRecipe(resourcekey, (recipedisplayentry) -> {
                    list.add(recipedisplayentry.id());
                });
            }
        }

        if (!list.isEmpty() && player.connection != null) {
            player.connection.send(new ClientboundRecipeBookRemovePacket(list));
        }

        return list.size();
    }
}
