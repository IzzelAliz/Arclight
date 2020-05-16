package io.izzel.arclight.mixin.core.inventory;

import io.izzel.arclight.bridge.inventory.IInventoryBridge;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_14_R1.entity.CraftHumanEntity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.InventoryHolder;
import org.spongepowered.asm.mixin.Mixin;

import java.util.List;

@Mixin(IInventory.class)
public interface IInventoryMixin extends IInventoryBridge {

    default List<ItemStack> getContents() {
        return bridge$getContents();
    }

    default void onOpen(CraftHumanEntity who) {
        bridge$onClose(who);
    }

    default void onClose(CraftHumanEntity who) {
        bridge$onClose(who);
    }

    default List<HumanEntity> getViewers() {
        return bridge$getViewers();
    }

    default InventoryHolder getOwner() {
        return bridge$getOwner();
    }

    default void setMaxStackSize(int size) {
        bridge$setMaxStackSize(size);
    }

    default Location getLocation() {
        return bridge$getLocation();
    }

    default IRecipe<?> getCurrentRecipe() {
        return bridge$getCurrentRecipe();
    }

    default void setCurrentRecipe(IRecipe<?> recipe) {
        bridge$setCurrentRecipe(recipe);
    }
}
