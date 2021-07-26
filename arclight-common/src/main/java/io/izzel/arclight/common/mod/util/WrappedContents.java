package io.izzel.arclight.common.mod.util;

import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class WrappedContents extends NonNullList<ItemStack> {

    private final Container inventory;

    public WrappedContents(Container inventory) {
        super(null, null);
        this.inventory = inventory;
    }

    @NotNull
    @Override
    public ItemStack get(int i) {
        return inventory.getItem(i);
    }

    @Override
    public ItemStack set(int i, ItemStack stack) {
        ItemStack ret = inventory.getItem(i);
        inventory.setItem(i, stack);
        return ret;
    }

    @Override
    public void add(int i, ItemStack stack) {
        if (inventory.getItem(i).isEmpty()) {
            inventory.setItem(i, stack);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public ItemStack remove(int i) {
        return inventory.removeItemNoUpdate(i);
    }

    @Override
    public int size() {
        return inventory.getContainerSize();
    }

    @Override
    public void clear() {
        inventory.clearContent();
    }
}
