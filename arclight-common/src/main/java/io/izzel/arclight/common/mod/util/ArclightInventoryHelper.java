package io.izzel.arclight.common.mod.util;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class ArclightInventoryHelper {

    private ArclightInventoryHelper() {
    }

    public static ItemStack getSelectedItem(Inventory inventory) {
        return inventory.getSelectedItem();
    }

    public static Item getSelectedItemType(Inventory inventory) {
        ItemStack stack = inventory.getSelectedItem();
        return stack.isEmpty() ? null : stack.getItem();
    }

    public static int getSelectedSlot(Inventory inventory) {
        return inventory.getSelectedSlot();
    }
}
