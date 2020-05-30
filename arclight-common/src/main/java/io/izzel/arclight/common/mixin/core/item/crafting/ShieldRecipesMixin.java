package io.izzel.arclight.common.mixin.core.item.crafting;

import io.izzel.arclight.common.bridge.item.crafting.IRecipeBridge;
import io.izzel.arclight.common.mod.util.ArclightSpecialRecipe;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.ShieldRecipes;
import net.minecraft.item.crafting.SpecialRecipe;
import net.minecraft.util.ResourceLocation;
import org.bukkit.craftbukkit.v.inventory.CraftItemStack;
import org.bukkit.inventory.Recipe;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ShieldRecipes.class)
public abstract class ShieldRecipesMixin extends SpecialRecipe implements IRecipeBridge {

    public ShieldRecipesMixin(ResourceLocation idIn) {
        super(idIn);
    }

    @Override
    public Recipe bridge$toBukkitRecipe() {
        return ArclightSpecialRecipe.shapeless(CraftItemStack.asCraftMirror(new ItemStack(Items.SHIELD)), this,
            Ingredient.fromItems(Items.WHITE_BANNER));
    }
}
