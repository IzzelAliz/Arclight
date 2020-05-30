package io.izzel.arclight.common.mixin.core.item.crafting;

import io.izzel.arclight.common.bridge.item.crafting.IRecipeBridge;
import net.minecraft.item.Items;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.SpecialRecipe;
import net.minecraft.item.crafting.SuspiciousStewRecipe;
import net.minecraft.util.ResourceLocation;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.spongepowered.asm.mixin.Mixin;
import io.izzel.arclight.common.mod.util.ArclightSpecialRecipe;

@Mixin(SuspiciousStewRecipe.class)
public abstract class SuspiciousStewRecipeMixin extends SpecialRecipe implements IRecipeBridge {

    public SuspiciousStewRecipeMixin(ResourceLocation idIn) {
        super(idIn);
    }

    @Override
    public Recipe bridge$toBukkitRecipe() {
        return ArclightSpecialRecipe.shapeless(new ItemStack(Material.SUSPICIOUS_STEW), this, Ingredient.fromItems(Items.BOWL));
    }
}
