package io.izzel.arclight.common.mod.server.world.inventory;

import com.google.common.base.Preconditions;
import io.izzel.arclight.common.mod.server.ArclightServer;
import net.minecraft.world.Container;
import org.bukkit.craftbukkit.v.inventory.CraftInventory;
import org.bukkit.inventory.ItemStack;

public class ArclightModdedMenuInventory extends CraftInventory {
    public ArclightModdedMenuInventory(Container inventory) {
        super(inventory);
    }

    @Override
    public void setStorageContents(ItemStack[] items) throws IllegalArgumentException {
        setContents(items);
    }

    @Override
    public void setContents(ItemStack[] items) {
        Preconditions.checkArgument(items.length <= this.getSize(), "Invalid inventory size (%s); expected %s or less", items.length, this.getSize());
        ArclightServer.LOGGER.debug("Skipping content override for a modded inventory");
    }

    @Override
    public void clear() {
        ArclightServer.LOGGER.debug("Skipping clear all for a modded inventory");
    }
}
