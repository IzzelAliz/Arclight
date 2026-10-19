package io.izzel.arclight.common.mixin.core.world.item.crafting;

import io.izzel.arclight.common.bridge.core.world.item.crafting.RecipeBridge;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.inventory.CraftComplexRecipe;
import org.bukkit.craftbukkit.inventory.CraftRecipe;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(CustomRecipe.class)
public abstract class CustomRecipeMixin implements RecipeBridge {

    // @formatter:off
    @Shadow public abstract String group();
    @Shadow public abstract CraftingBookCategory category();
    // @formatter:on

    @Override
    public Recipe bridge$toBukkitRecipe(NamespacedKey id) {
        CraftComplexRecipe recipe = new CraftComplexRecipe(id, new ItemStack(Material.AIR), (CustomRecipe) (Object) this);
        recipe.setGroup(this.group());
        recipe.setCategory(CraftRecipe.getCategory(this.category()));
        return recipe;
    }
}
