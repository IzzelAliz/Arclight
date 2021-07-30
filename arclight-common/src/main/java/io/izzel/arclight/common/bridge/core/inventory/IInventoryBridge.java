package io.izzel.arclight.common.bridge.core.inventory;

import io.izzel.arclight.common.mod.ArclightMod;
import io.izzel.arclight.common.mod.util.WrappedContents;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v.entity.CraftHumanEntity;
import org.bukkit.craftbukkit.v.inventory.CraftInventory;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.List;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;

public interface IInventoryBridge {

    int MAX_STACK = 64;

    default List<ItemStack> getContents() {
        return new WrappedContents((Container) this);
    }

    void onOpen(CraftHumanEntity who);

    void onClose(CraftHumanEntity who);

    List<HumanEntity> getViewers();

    InventoryHolder getOwner();

    void setOwner(InventoryHolder owner);

    void setMaxStackSize(int size);

    Location getLocation();

    default Recipe<?> getCurrentRecipe() {
        return null;
    }

    default void setCurrentRecipe(Recipe<?> recipe) {
    }

    default Inventory getOwnerInventory() {
        InventoryHolder owner = this.getOwner();
        if (owner != null) {
            return owner.getInventory();
        } else {
            ArclightMod.LOGGER.warn("No owner for inventory {}/{}", this, this.getClass());
            return new CraftInventory((Container) this);
        }
    }
}
