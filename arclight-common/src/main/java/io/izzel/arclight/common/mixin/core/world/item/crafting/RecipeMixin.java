package io.izzel.arclight.common.mixin.core.world.item.crafting;

import io.izzel.arclight.common.bridge.core.world.item.crafting.RecipeBridge;
import io.izzel.arclight.common.mod.util.ArclightSpecialRecipe;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.Recipe;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(net.minecraft.world.item.crafting.Recipe.class)
public interface RecipeMixin extends RecipeBridge {

    default Recipe toBukkitRecipe(NamespacedKey id) {
        return bridge$toBukkitRecipe(id);
    }

    @Override
    default Recipe bridge$toBukkitRecipe(NamespacedKey id) {
        return new ArclightSpecialRecipe(id, (net.minecraft.world.item.crafting.Recipe<?>) this);
    }
}
