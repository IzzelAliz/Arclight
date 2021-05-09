package io.izzel.arclight.common.mod.util;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class WrappedContents extends NonNullList<ItemStack> {

    private final IInventory inventory;

    public WrappedContents(IInventory inventory) {
        this.inventory = inventory;
    }

    @NotNull
    @Override
    public ItemStack get(int i) {
        return inventory.getStackInSlot(i);
    }

    @Override
    public ItemStack set(int i, ItemStack stack) {
        ItemStack ret = inventory.getStackInSlot(i);
        inventory.setInventorySlotContents(i, stack);
        return ret;
    }

    @Override
    public void add(int i, ItemStack stack) {
        if (inventory.getStackInSlot(i).isEmpty()) {
            inventory.setInventorySlotContents(i, stack);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public ItemStack remove(int i) {
        return inventory.removeStackFromSlot(i);
    }

    @Override
    public int size() {
        return inventory.getSizeInventory();
    }

    @Override
    public void clear() {
        inventory.clear();
    }
}
