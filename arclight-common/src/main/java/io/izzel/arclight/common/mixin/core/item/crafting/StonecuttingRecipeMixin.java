package io.izzel.arclight.common.mixin.core.item.crafting;

import io.izzel.arclight.common.bridge.item.crafting.IRecipeBridge;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.SingleItemRecipe;
import net.minecraft.item.crafting.StonecuttingRecipe;
import net.minecraft.util.ResourceLocation;
import org.bukkit.craftbukkit.v.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v.inventory.CraftRecipe;
import org.bukkit.craftbukkit.v.inventory.CraftStonecuttingRecipe;
import org.bukkit.craftbukkit.v.util.CraftNamespacedKey;
import org.bukkit.inventory.Recipe;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(StonecuttingRecipe.class)
public abstract class StonecuttingRecipeMixin extends SingleItemRecipe implements IRecipeBridge {

    public StonecuttingRecipeMixin(IRecipeType<?> type, IRecipeSerializer<?> serializer, ResourceLocation id, String group, Ingredient ingredient, ItemStack result) {
        super(type, serializer, id, group, ingredient, result);
    }

    @Override
    public Recipe bridge$toBukkitRecipe() {
        CraftItemStack result = CraftItemStack.asCraftMirror(this.result);
        CraftStonecuttingRecipe recipe = new CraftStonecuttingRecipe(CraftNamespacedKey.fromMinecraft(this.getId()), result, CraftRecipe.toBukkit(this.ingredient));
        recipe.setGroup(this.group);
        return recipe;
    }
}
