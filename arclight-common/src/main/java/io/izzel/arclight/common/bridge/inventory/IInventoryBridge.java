package io.izzel.arclight.common.bridge.inventory;

import io.izzel.arclight.common.mod.ArclightMod;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v.entity.CraftHumanEntity;
import org.bukkit.craftbukkit.v.inventory.CraftInventory;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.List;

public interface IInventoryBridge {

    int MAX_STACK = 64;

    List<ItemStack> getContents();

    void onOpen(CraftHumanEntity who);

    void onClose(CraftHumanEntity who);

    List<HumanEntity> getViewers();

    InventoryHolder getOwner();

    void setOwner(InventoryHolder owner);

    void setMaxStackSize(int size);

    Location getLocation();

    IRecipe<?> getCurrentRecipe();

    void setCurrentRecipe(IRecipe<?> recipe);

    default Inventory getOwnerInventory() {
        InventoryHolder owner = this.getOwner();
        if (owner != null) {
            return owner.getInventory();
        } else {
            ArclightMod.LOGGER.warn("No owner for inventory {}/{}", this, this.getClass());
            return new CraftInventory((IInventory) this);
        }
    }
}
