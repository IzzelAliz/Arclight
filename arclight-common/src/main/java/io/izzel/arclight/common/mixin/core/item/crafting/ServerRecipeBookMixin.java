package io.izzel.arclight.common.mixin.core.item.crafting;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.ServerRecipeBook;
import net.minecraft.network.play.server.SRecipeBookPacket;
import net.minecraft.util.ResourceLocation;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;
import java.util.List;

@Mixin(ServerRecipeBook.class)
public class ServerRecipeBookMixin {

    @Redirect(method = "add", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/crafting/IRecipe;isDynamic()Z"))
    public boolean arclight$recipeUpdate(IRecipe<?> recipe, Collection<IRecipe<?>> collection, ServerPlayerEntity playerEntity) {
        return !recipe.isDynamic() && CraftEventFactory.handlePlayerRecipeListUpdateEvent(playerEntity, recipe.getId());
    }

    @Inject(method = "sendPacket", cancellable = true, at = @At("HEAD"))
    public void arclight$returnIfFail(SRecipeBookPacket.State state, ServerPlayerEntity player, List<ResourceLocation> recipesIn, CallbackInfo ci) {
        if (player.connection == null) {
            ci.cancel();
        }
    }
}
