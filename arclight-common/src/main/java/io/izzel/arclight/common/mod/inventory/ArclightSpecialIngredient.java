package io.izzel.arclight.common.mod.inventory;

import net.minecraft.world.item.crafting.Ingredient;
import org.bukkit.Material;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class ArclightSpecialIngredient implements RecipeChoice {

    private final Ingredient ingredient;

    public ArclightSpecialIngredient(Ingredient ingredient) {
        this.ingredient = ingredient;
    }

    public Ingredient getIngredient() {
        return ingredient;
    }

    @NotNull
    @Override
    public ItemStack getItemStack() {
        var items = ingredient.items().toList();
        return !items.isEmpty()
            ? CraftItemStack.asCraftMirror(new net.minecraft.world.item.ItemStack(items.getFirst().value()))
            : new ItemStack(Material.AIR, 0);
    }

    @NotNull
    @Override
    public RecipeChoice clone() {
        try {
            return (RecipeChoice) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public boolean test(@NotNull ItemStack itemStack) {
        return ingredient.test(CraftItemStack.asNMSCopy(itemStack));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ArclightSpecialIngredient that = (ArclightSpecialIngredient) o;
        return Objects.equals(ingredient, that.ingredient);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ingredient);
    }
}
