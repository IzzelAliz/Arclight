package io.izzel.arclight.common.bridge.core.world;

import io.izzel.arclight.common.mod.util.WrappedContents;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v.entity.CraftHumanEntity;
import org.bukkit.craftbukkit.v.inventory.CraftInventory;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;

import java.util.List;

public interface IInventoryBridge {

    // // Default Arclight stack limit
    int MAX_STACK = 99;

    // // Define the getter so it's visible to the event factory
    default InventoryView getBukkitView() {
        return null;
    }

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

    default RecipeHolder<?> getCurrentRecipe() {
        return null;
    }

    default void setCurrentRecipe(RecipeHolder<?> recipe) {
    }

    default Inventory getOwnerInventory() {
        InventoryHolder owner = this.getOwner();
        if (owner != null) {
            return owner.getInventory();
        } else {
            // // Create a wrapper inventory if no owner exists
            return new CraftInventory((Container) this);
        }
    }

    // // Helper method used by Mixins to reference the Bukkit inventory
    default Inventory bridge$getBukkitInventory() {
        return getOwnerInventory();
    }
}
