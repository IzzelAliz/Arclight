package io.izzel.arclight.common.mixin.core.server.network;

import com.destroystokyo.paper.event.player.PlayerRecipeBookClickEvent;
import io.izzel.arclight.common.bridge.core.server.level.ServerPlayerBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ServerboundPlaceRecipePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.inventory.RecipeBookMenu;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.craftbukkit.util.CraftNamespacedKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerRecipeBookEventMixin {

    @Shadow public ServerPlayer player;

    // Replaces the old listener's arclight$recipeBookClick decorator when registered.
    // Both Paper and the retained legacy Bukkit chain must have exactly one producer.
    @Decorate(method = "handlePlaceRecipe(Lnet/minecraft/network/protocol/game/ServerboundPlaceRecipePacket;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/game/ServerboundPlaceRecipePacket;useMaxItems()Z"),
        require = 1, allow = 1)
    private boolean arclight$paperRecipeBookClick(ServerboundPlaceRecipePacket packet,
                                                 @Local(ordinal = 0) RecipeHolder<?> holder) throws Throwable {
        // This is after all original-recipe acceptance checks, but before creative,
        // level and inventory arguments are read for the native placement call.
        boolean makeAll = (boolean) DecorationOps.callsite().invoke(packet);
        var recipeName = CraftNamespacedKey.fromResourceKey(holder.id());
        var paperEvent = new PlayerRecipeBookClickEvent(((ServerPlayerBridge) this.player).bridge$getBukkitEntity(), recipeName, makeAll);
        if (!paperEvent.callEvent()) {
            return (boolean) DecorationOps.cancel().invoke();
        }
        recipeName = paperEvent.getRecipe();
        makeAll = paperEvent.isMakeAll();

        if (org.bukkit.event.player.PlayerRecipeBookClickEvent.getHandlerList().getRegisteredListeners().length > 0) {
            var bukkitRecipe = Bukkit.getRecipe(recipeName);
            if (bukkitRecipe == null) {
                return (boolean) DecorationOps.cancel().invoke();
            }
            var legacyEvent = CraftEventFactory.callRecipeBookClickEvent(this.player, bukkitRecipe, makeAll);
            recipeName = ((Keyed) legacyEvent.getRecipe()).getKey();
            makeAll = legacyEvent.isShiftClick();
        }
        if (!(this.player.containerMenu instanceof RecipeBookMenu)) {
            return (boolean) DecorationOps.cancel().invoke();
        }
        holder = this.player.level().getServer().getRecipeManager()
            .byKey(CraftNamespacedKey.toResourceKey(Registries.RECIPE, recipeName)).orElse(null);
        if (holder == null) {
            return (boolean) DecorationOps.cancel().invoke();
        }
        // The native placement reads this very local after the decorated accessor.
        DecorationOps.blackhole().invoke(holder);
        return makeAll;
    }
}
