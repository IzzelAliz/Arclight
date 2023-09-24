package io.izzel.arclight.common.mixin.core.world.item.crafting;

import com.google.common.collect.Lists;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.network.protocol.game.ClientboundRecipePacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.RecipeBook;
import net.minecraft.stats.ServerRecipeBook;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;
import java.util.List;

@Mixin(ServerRecipeBook.class)
public abstract class ServerRecipeBookMixin extends RecipeBook {

    // @formatter:off
    @Shadow protected abstract void sendRecipes(ClientboundRecipePacket.State p_12802_, ServerPlayer p_12803_, List<ResourceLocation> p_12804_);
    // @formatter:on

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public int addRecipes(Collection<RecipeHolder<?>> p_12792_, ServerPlayer p_12793_) {
        List<ResourceLocation> list = Lists.newArrayList();
        int i = 0;

        for (RecipeHolder<?> recipeholder : p_12792_) {
            ResourceLocation resourcelocation = recipeholder.id();
            if (!this.known.contains(resourcelocation) && !recipeholder.value().isSpecial() && CraftEventFactory.handlePlayerRecipeListUpdateEvent(p_12793_, resourcelocation)) {
                this.add(resourcelocation);
                this.addHighlight(resourcelocation);
                list.add(resourcelocation);
                CriteriaTriggers.RECIPE_UNLOCKED.trigger(p_12793_, recipeholder);
                ++i;
            }
        }

        if (list.size() > 0) {
            this.sendRecipes(ClientboundRecipePacket.State.ADD, p_12793_, list);
        }

        return i;
    }

    @Inject(method = "sendRecipes", cancellable = true, at = @At("HEAD"))
    public void arclight$returnIfFail(ClientboundRecipePacket.State state, ServerPlayer player, List<ResourceLocation> recipesIn, CallbackInfo ci) {
        if (player.connection == null) {
            ci.cancel();
        }
    }
}
