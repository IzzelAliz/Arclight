package io.izzel.arclight.common.mixin.core.item.crafting;

import io.izzel.arclight.common.bridge.item.crafting.IRecipeBridge;
import net.minecraft.item.Items;
import net.minecraft.item.crafting.FireworkStarFadeRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.SpecialRecipe;
import net.minecraft.util.ResourceLocation;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.spongepowered.asm.mixin.Mixin;
import io.izzel.arclight.common.mod.util.ArclightSpecialRecipe;

@Mixin(FireworkStarFadeRecipe.class)
public abstract class FireworkStarFadeRecipeMixin extends SpecialRecipe implements IRecipeBridge {

    public FireworkStarFadeRecipeMixin(ResourceLocation idIn) {
        super(idIn);
    }

    @Override
    public Recipe bridge$toBukkitRecipe() {
        return ArclightSpecialRecipe.shapeless(new ItemStack(Material.FIREWORK_STAR), this,
            Ingredient.fromItems(Items.FIREWORK_STAR, Items.BONE_MEAL));
    }
}
