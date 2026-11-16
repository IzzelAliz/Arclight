package io.izzel.arclight.common.bridge.core.world.item.crafting;

import net.minecraft.world.item.ItemStack;

import java.util.List;

public interface IngredientBridge {

    void bridge$setExactChoices(List<ItemStack> exactChoices);

    List<ItemStack> bridge$getExactChoices();

    boolean bridge$isExact();

    void bridge$setStackPredicate(java.util.function.Predicate<ItemStack> predicate);

    java.util.function.Predicate<ItemStack> bridge$getStackPredicate();

    // Vanilla/Fabric have no native custom predicate; NeoForge overrides this bridge.
    default boolean bridge$isCustom() {
        return false;
    }
}
