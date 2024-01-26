package io.izzel.arclight.common.mixin.core.world.item.crafting;

import io.izzel.arclight.common.bridge.core.world.item.crafting.RecipeBridge;
import io.izzel.arclight.common.mod.util.ArclightSpecialRecipe;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.CampfireCookingRecipe;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeType;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.v.inventory.CraftCampfireRecipe;
import org.bukkit.craftbukkit.v.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v.inventory.CraftRecipe;
import org.bukkit.inventory.Recipe;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(CampfireCookingRecipe.class)
public abstract class CampfireCookingRecipeMixin extends AbstractCookingRecipe implements RecipeBridge {

    public CampfireCookingRecipeMixin(RecipeType<?> p_250197_, String p_249518_, CookingBookCategory p_250891_, Ingredient p_251354_, ItemStack p_252185_, float p_252165_, int p_250256_) {
        super(p_250197_, p_249518_, p_250891_, p_251354_, p_252185_, p_252165_, p_250256_);
    }

    @Override
    public Recipe bridge$toBukkitRecipe(NamespacedKey id) {
        if (this.result.isEmpty()) {
            return new ArclightSpecialRecipe(id, this);
        }
        CraftItemStack result = CraftItemStack.asCraftMirror(this.result);
        CraftCampfireRecipe recipe = new CraftCampfireRecipe(id, result, CraftRecipe.toBukkit(this.ingredient), this.experience, this.cookingTime);
        recipe.setGroup(this.group);
        recipe.setCategory(CraftRecipe.getCategory(this.category()));
        return recipe;
    }
}
