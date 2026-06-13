package io.izzel.arclight.common.mixin.bukkit;

import io.izzel.arclight.common.bridge.core.world.item.crafting.IngredientBridge;
import io.izzel.arclight.common.mod.inventory.ArclightSpecialIngredient;
import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.craftbukkit.inventory.CraftRecipe;
import org.bukkit.craftbukkit.util.CraftMagicNumbers;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.util.ArrayList;
import java.util.List;

@Mixin(value = CraftRecipe.class, remap = false)
public interface CraftRecipeMixin {

    /**
     * @author IzzelAliz
     * @reason 26.1 Ingredient HolderSet API
     */
    @Overwrite
    default Ingredient toNMS(RecipeChoice bukkit, boolean requireNotEmpty) {
        Ingredient stack;
        if (bukkit == null) {
            stack = Ingredient.of();
        } else if (bukkit instanceof RecipeChoice.MaterialChoice materialChoice) {
            stack = Ingredient.of(materialChoice.getChoices().stream().map(mat -> CraftItemStack.asNMSCopy(new ItemStack(mat)).getItem()));
        } else if (bukkit instanceof RecipeChoice.ExactChoice exactChoice) {
            stack = Ingredient.of(exactChoice.getChoices().stream().map(CraftItemStack::asNMSCopy).map(net.minecraft.world.item.ItemStack::getItem));
            ((IngredientBridge) (Object) stack).bridge$setExact(true);
        } else if (bukkit instanceof ArclightSpecialIngredient special) {
            stack = special.getIngredient();
        } else {
            throw new IllegalArgumentException("Unknown recipe stack instance " + bukkit);
        }

        if (stack.getClass() == Ingredient.class && requireNotEmpty && stack.isEmpty()) {
            throw new IllegalArgumentException("Recipe requires at least one non-air choice!");
        }
        return stack;
    }

    /**
     * @author IzzelAliz
     * @reason 26.1 Ingredient HolderSet API
     */
    @Overwrite
    static RecipeChoice toBukkit(Ingredient list) {
        if (list.getClass() != Ingredient.class) {
            return new ArclightSpecialIngredient(list);
        }
        List<net.minecraft.world.item.ItemStack> items = list.items()
            .map(Holder::value)
            .map(Item::getDefaultInstance)
            .toList();
        if (items.isEmpty()) {
            return null;
        }
        if (((IngredientBridge) (Object) list).bridge$isExact()) {
            List<ItemStack> choices = new ArrayList<>(items.size());
            for (net.minecraft.world.item.ItemStack i : items) {
                choices.add(CraftItemStack.asBukkitCopy(i));
            }
            return new RecipeChoice.ExactChoice(choices);
        }
        List<org.bukkit.Material> choices = new ArrayList<>(items.size());
        for (net.minecraft.world.item.ItemStack i : items) {
            choices.add(CraftMagicNumbers.getMaterial(i.getItem()));
        }
        return new RecipeChoice.MaterialChoice(choices);
    }
}
