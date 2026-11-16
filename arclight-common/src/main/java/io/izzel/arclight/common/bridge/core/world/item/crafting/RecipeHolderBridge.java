package io.izzel.arclight.common.bridge.core.world.item.crafting;

import net.minecraft.world.item.crafting.RecipeHolder;
import org.bukkit.inventory.Recipe;

public interface RecipeHolderBridge {

    default Recipe toBukkitRecipe() {
        return this.bridge$toBukkitRecipe();
    }

    Recipe bridge$toBukkitRecipe();

    boolean bridge$isRuntimeAddedRecipe();

    RecipeHolder<?> bridge$asRuntimeAddedRecipe();
}
