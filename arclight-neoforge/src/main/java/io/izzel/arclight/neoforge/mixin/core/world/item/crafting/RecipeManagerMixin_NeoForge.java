package io.izzel.arclight.neoforge.mixin.core.world.item.crafting;

import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.server.level.ServerPlayer;
import io.izzel.arclight.common.bridge.core.world.item.crafting.RecipeManagerBridge;
import io.izzel.arclight.neoforge.mod.util.RecipeContentSubscriptions;
import org.spongepowered.asm.mixin.Mixin;

/**
 * NeoForge applies recipe conditions directly in RecipeManager.prepare via
 * ConditionalOps and its reload-listener context. RecipeManager no longer
 * extends SimpleJsonResourceReloadListener in 1.21.11, so this mixin must not
 * retain that obsolete superclass or its constructor.
 */
@Mixin(RecipeManager.class)
public abstract class RecipeManagerMixin_NeoForge implements RecipeManagerBridge {
    @Override
    public void bridge$syncModRecipeContent(ServerPlayer player) {
        RecipeContentSubscriptions.refresh(player, ((RecipeManager) (Object) this).recipeMap());
    }
}
