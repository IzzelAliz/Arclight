package io.izzel.arclight.common.mixin.core.item.crafting;

import io.izzel.arclight.common.bridge.item.crafting.IRecipeBridge;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.AbstractCookingRecipe;
import net.minecraft.item.crafting.BlastingRecipe;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.ResourceLocation;
import org.bukkit.craftbukkit.v.inventory.CraftBlastingRecipe;
import org.bukkit.craftbukkit.v.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v.inventory.CraftRecipe;
import org.bukkit.craftbukkit.v.util.CraftNamespacedKey;
import org.bukkit.inventory.Recipe;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(BlastingRecipe.class)
public abstract class BlastingRecipeMixin extends AbstractCookingRecipe implements IRecipeBridge {

    public BlastingRecipeMixin(IRecipeType<?> typeIn, ResourceLocation idIn, String groupIn, Ingredient ingredientIn, ItemStack resultIn, float experienceIn, int cookTimeIn) {
        super(typeIn, idIn, groupIn, ingredientIn, resultIn, experienceIn, cookTimeIn);
    }

    @Override
    public Recipe bridge$toBukkitRecipe() {
        CraftItemStack result = CraftItemStack.asCraftMirror(this.result);
        CraftBlastingRecipe recipe = new CraftBlastingRecipe(CraftNamespacedKey.fromMinecraft(this.getId()), result, CraftRecipe.toBukkit(this.ingredient), this.experience, this.cookTime);
        recipe.setGroup(this.group);
        return recipe;
    }
}
