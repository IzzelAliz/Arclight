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
    public void setContents(ItemStack[] items) {
        ArclightServer.LOGGER.debug("Overriding content for a modded container menu inventory");
        super.setContents(items);
    }

    @Override
    public void clear() {
        ArclightServer.LOGGER.debug("Clearing everything for a modded container menu inventory");
        super.clear();
    }
}
