package io.izzel.arclight.common.mixin.core.world.item.crafting;

import io.izzel.arclight.common.bridge.core.world.item.crafting.RecipeBridge;
import net.minecraft.world.item.crafting.CustomRecipe;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.Recipe;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(CustomRecipe.class)
public class CustomRecipeMixin implements RecipeBridge {

    @Override
    public Recipe bridge$toBukkitRecipe(NamespacedKey id) {
        return new org.bukkit.craftbukkit.v.inventory.CraftComplexRecipe(id, (CustomRecipe) (Object) this);
    }
}
