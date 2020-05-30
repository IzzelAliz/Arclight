package io.izzel.arclight.common.mixin.core.item.crafting;

import io.izzel.arclight.common.bridge.item.crafting.IRecipeBridge;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.ShapelessRecipe;
import net.minecraft.util.NonNullList;
import org.bukkit.craftbukkit.v.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v.inventory.CraftRecipe;
import org.bukkit.craftbukkit.v.inventory.CraftShapelessRecipe;
import org.bukkit.inventory.Recipe;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ShapelessRecipe.class)
public class ShapelessRecipeMixin implements IRecipeBridge {

    // @formatter:off
    @Shadow @Final private ItemStack recipeOutput;
    @Shadow @Final private String group;
    @Shadow @Final private NonNullList<Ingredient> recipeItems;
    // @formatter:off

    @Override
    public Recipe bridge$toBukkitRecipe() {
        CraftItemStack result = CraftItemStack.asCraftMirror(this.recipeOutput);
        CraftShapelessRecipe recipe = new CraftShapelessRecipe(result, (ShapelessRecipe)(Object) this);
        recipe.setGroup(this.group);
        for (Ingredient list : this.recipeItems) {
            recipe.addIngredient(CraftRecipe.toBukkit(list));
        }
        return recipe;
    }
}
