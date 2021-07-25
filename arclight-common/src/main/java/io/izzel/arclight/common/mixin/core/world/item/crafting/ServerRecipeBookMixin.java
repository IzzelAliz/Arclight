package io.izzel.arclight.common.mixin.core.world.item.crafting;

import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;
import java.util.List;
import net.minecraft.network.protocol.game.ClientboundRecipePacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.ServerRecipeBook;
import net.minecraft.world.item.crafting.Recipe;

@Mixin(ServerRecipeBook.class)
public class ServerRecipeBookMixin {

    @Redirect(method = "addRecipes", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/crafting/Recipe;isSpecial()Z"))
    public boolean arclight$recipeUpdate(Recipe<?> recipe, Collection<Recipe<?>> collection, ServerPlayer playerEntity) {
        return recipe.isSpecial() || !CraftEventFactory.handlePlayerRecipeListUpdateEvent(playerEntity, recipe.getId());
    }

    @Inject(method = "sendRecipes", cancellable = true, at = @At("HEAD"))
    public void arclight$returnIfFail(ClientboundRecipePacket.State state, ServerPlayer player, List<ResourceLocation> recipesIn, CallbackInfo ci) {
        if (player.connection == null) {
            ci.cancel();
        }
    }
}
