package io.izzel.arclight.common.mixin.core.world.item.crafting;

import io.izzel.arclight.common.bridge.core.world.item.crafting.RecipeBridge;
import io.izzel.arclight.common.mod.util.ArclightSpecialRecipe;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleItemRecipe;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import org.bukkit.NamespacedKey;
import org.bukkit.craftbukkit.v.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v.inventory.CraftRecipe;
import org.bukkit.craftbukkit.v.inventory.CraftStonecuttingRecipe;
import org.bukkit.inventory.Recipe;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(StonecutterRecipe.class)
public abstract class StonecuttingRecipeMixin extends SingleItemRecipe implements RecipeBridge {

    public StonecuttingRecipeMixin(RecipeType<?> p_44416_, RecipeSerializer<?> p_44417_, String p_44419_, Ingredient p_44420_, ItemStack p_44421_) {
        super(p_44416_, p_44417_, p_44419_, p_44420_, p_44421_);
    }

    @Override
    public Recipe bridge$toBukkitRecipe(NamespacedKey id) {
        if (this.result.isEmpty()) {
            return new ArclightSpecialRecipe(id, this);
        }
        CraftItemStack result = CraftItemStack.asCraftMirror(this.result);
        CraftStonecuttingRecipe recipe = new CraftStonecuttingRecipe(id, result, CraftRecipe.toBukkit(this.ingredient));
        recipe.setGroup(this.group);
        return recipe;
    }
}
