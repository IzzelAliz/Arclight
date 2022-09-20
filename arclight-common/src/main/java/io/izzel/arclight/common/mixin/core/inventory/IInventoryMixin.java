package io.izzel.arclight.common.mixin.core.inventory;

import io.izzel.arclight.common.bridge.inventory.IInventoryBridge;
import io.izzel.arclight.common.mod.inventory.SideViewingTracker;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.crafting.IRecipe;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v.entity.CraftHumanEntity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.InventoryHolder;
import org.spongepowered.asm.mixin.Mixin;

import java.util.List;

@Mixin(IInventory.class)
public interface IInventoryMixin extends IInventoryBridge {

    @Override
    default void onOpen(CraftHumanEntity who) {
    }

    @Override
    default void onClose(CraftHumanEntity who) {
    }

    @Override
    default List<HumanEntity> getViewers() {
        return SideViewingTracker.getViewers((IInventory) this);
    }

    @Override
    default InventoryHolder getOwner() {
        return null;
    }

    @Override
    default void setMaxStackSize(int size) {
    }

    @Override
    default Location getLocation() {
        return null;
    }

    @Override
    default IRecipe<?> getCurrentRecipe() {
        return null;
    }

    @Override
    default void setCurrentRecipe(IRecipe<?> recipe) {
    }
}
