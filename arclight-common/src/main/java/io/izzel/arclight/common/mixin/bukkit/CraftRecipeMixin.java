package io.izzel.arclight.common.mixin.bukkit;

import io.izzel.arclight.common.bridge.core.world.item.crafting.IngredientBridge;
import io.izzel.arclight.common.mod.inventory.ArclightSpecialIngredient;
import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import org.bukkit.craftbukkit.v.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v.inventory.CraftRecipe;
import org.bukkit.craftbukkit.v.util.CraftMagicNumbers;
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
     * @reason
     */
    @Overwrite
    default Ingredient toNMS(RecipeChoice bukkit, boolean requireNotEmpty) {
        Ingredient stack;
        if (bukkit == null) {
            stack = Ingredient.of();
        } else if (bukkit instanceof RecipeChoice.MaterialChoice) {
            stack = Ingredient.of(((RecipeChoice.MaterialChoice) bukkit).getChoices().stream().map(CraftMagicNumbers::getItem));
        } else if (bukkit instanceof RecipeChoice.ExactChoice) {
            List<net.minecraft.world.item.ItemStack> exactChoices = ((RecipeChoice.ExactChoice) bukkit).getChoices().stream()
                .map(CraftItemStack::asNMSCopy).toList();
            stack = Ingredient.of(exactChoices.stream().map(net.minecraft.world.item.ItemStack::getItem));
            ((IngredientBridge) (Object) stack).bridge$setExactChoices(exactChoices);
        } else if (bukkit instanceof ArclightSpecialIngredient) {
            stack = ((ArclightSpecialIngredient) bukkit).getIngredient();
        } else {
            throw new IllegalArgumentException("Unknown recipe stack instance " + bukkit);
        }

        if (stack.getClass() == Ingredient.class && requireNotEmpty && stack.isEmpty()) {
            throw new IllegalArgumentException("Recipe requires at least one non-air choice!");
        } else {
            return stack;
        }
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    static RecipeChoice toBukkit(Ingredient list) {
        if (list.getClass() != Ingredient.class) {
            return new ArclightSpecialIngredient(list);
        }
        List<Holder<Item>> items = list.items().toList();
        if (items.isEmpty()) {
            return null;
        } else {
            if (((IngredientBridge) (Object) list).bridge$isExact()) {
                List<net.minecraft.world.item.ItemStack> exactChoices = ((IngredientBridge) (Object) list).bridge$getExactChoices();
                List<ItemStack> choices = exactChoices.stream().map(CraftItemStack::asBukkitCopy).toList();
                return new RecipeChoice.ExactChoice(choices);
            } else {
                List<org.bukkit.Material> choices = new ArrayList<>(items.size());
                for (Holder<Item> i : items) {
                    choices.add(CraftMagicNumbers.getMaterial(i.value()));
                }
                return new RecipeChoice.MaterialChoice(choices);
            }
        }
    }
}
