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

    // // Global Arclight max stack constant
    int MAX_STACK = 99;

    // // Added to bridge Forge trading events to Bukkit InventoryViews
    // // Using the base InventoryView interface to avoid version-specific crashes
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
            // // Create a generic Bukkit inventory if there's no owner mod/entity
            return new CraftInventory((Container) this);
        }
    }

    // // Helper method to satisfy Mixins calling for a Bukkit inventory handle
    default Inventory bridge$getBukkitInventory() {
        return getOwnerInventory();
    }
}
