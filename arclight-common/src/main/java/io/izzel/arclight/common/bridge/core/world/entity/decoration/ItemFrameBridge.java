package io.izzel.arclight.common.bridge.core.world.entity.decoration;

import net.minecraft.world.item.ItemStack;

public interface ItemFrameBridge {

    default void setItem(ItemStack stack, boolean updateNeighbours, boolean playSound) {
        throw new UnsupportedOperationException("Implemented by ItemFrameMixin");
    }
}
