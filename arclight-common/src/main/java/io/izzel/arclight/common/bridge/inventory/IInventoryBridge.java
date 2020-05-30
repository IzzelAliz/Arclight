package io.izzel.arclight.common.bridge.inventory;

import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v.entity.CraftHumanEntity;
import org.bukkit.entity.HumanEntity;
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
}
