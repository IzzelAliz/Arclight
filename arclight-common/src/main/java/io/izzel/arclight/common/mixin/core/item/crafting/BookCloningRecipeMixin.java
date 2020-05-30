package io.izzel.arclight.common.mixin.core.item.crafting;

import io.izzel.arclight.common.bridge.item.crafting.IRecipeBridge;
import io.izzel.arclight.common.mod.util.ArclightSpecialRecipe;
import net.minecraft.item.Items;
import net.minecraft.item.crafting.BookCloningRecipe;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(BookCloningRecipe.class)
public class BookCloningRecipeMixin implements IRecipeBridge {

    @Override
    public Recipe bridge$toBukkitRecipe() {
        return ArclightSpecialRecipe.shapeless(new ItemStack(Material.WRITTEN_BOOK), (IRecipe<?>) this, Ingredient.fromItems(Items.WRITTEN_BOOK));
    }
}
