package io.izzel.arclight.mixin.core.item.crafting;

import io.izzel.arclight.bridge.item.crafting.IRecipeBridge;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.AbstractCookingRecipe;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.SmokingRecipe;
import net.minecraft.util.ResourceLocation;
import org.bukkit.craftbukkit.v1_14_R1.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_14_R1.inventory.CraftRecipe;
import org.bukkit.craftbukkit.v1_14_R1.inventory.CraftSmokingRecipe;
import org.bukkit.craftbukkit.v1_14_R1.util.CraftNamespacedKey;
import org.bukkit.inventory.Recipe;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(SmokingRecipe.class)
public abstract class SmokingRecipeMixin extends AbstractCookingRecipe implements IRecipeBridge {

    public SmokingRecipeMixin(IRecipeType<?> typeIn, ResourceLocation idIn, String groupIn, Ingredient ingredientIn, ItemStack resultIn, float experienceIn, int cookTimeIn) {
        super(typeIn, idIn, groupIn, ingredientIn, resultIn, experienceIn, cookTimeIn);
    }

    @Override
    public Recipe bridge$toBukkitRecipe() {
        CraftItemStack result = CraftItemStack.asCraftMirror(this.result);
        CraftSmokingRecipe recipe = new CraftSmokingRecipe(CraftNamespacedKey.fromMinecraft(this.getId()), result, CraftRecipe.toBukkit(this.ingredient), this.experience, this.cookTime);
        recipe.setGroup(this.group);
        return recipe;
    }
}
