package io.izzel.arclight.common.mixin.core.item.crafting;

import io.izzel.arclight.common.bridge.item.crafting.IRecipeBridge;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.SpecialRecipe;
import net.minecraft.item.crafting.TippedArrowRecipe;
import net.minecraft.util.ResourceLocation;
import org.bukkit.craftbukkit.v.inventory.CraftItemStack;
import org.bukkit.inventory.Recipe;
import org.spongepowered.asm.mixin.Mixin;
import io.izzel.arclight.common.mod.util.ArclightSpecialRecipe;

@Mixin(TippedArrowRecipe.class)
public abstract class TippedArrowRecipeMixin extends SpecialRecipe implements IRecipeBridge {

    public TippedArrowRecipeMixin(ResourceLocation idIn) {
        super(idIn);
    }

    @Override
    public Recipe bridge$toBukkitRecipe() {
        CraftItemStack result = CraftItemStack.asCraftMirror(new ItemStack(Items.TIPPED_ARROW, 8));
        return ArclightSpecialRecipe.shaped(result, this, 3,
            Ingredient.fromItems(Items.ARROW), Ingredient.fromItems(Items.ARROW), Ingredient.fromItems(Items.ARROW),
            Ingredient.fromItems(Items.ARROW), Ingredient.fromItems(Items.LINGERING_POTION), Ingredient.fromItems(Items.ARROW),
            Ingredient.fromItems(Items.ARROW), Ingredient.fromItems(Items.ARROW), Ingredient.fromItems(Items.ARROW)
        );
    }
}
