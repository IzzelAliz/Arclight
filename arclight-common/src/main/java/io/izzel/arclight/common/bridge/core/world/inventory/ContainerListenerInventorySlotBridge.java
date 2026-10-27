package io.izzel.arclight.common.bridge.core.world.inventory;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/** Carries the previous slot snapshot through an unchanged native listener call. */
public interface ContainerListenerInventorySlotBridge {

    /** Installs a callback-local change, returning the previous context for finally restoration. */
    @Nullable
    SlotChange bridge$swapInventorySlotChange(@Nullable SlotChange change);

    record SlotChange(AbstractContainerMenu container, int rawSlot, ItemStack oldStack) {
    }
}
