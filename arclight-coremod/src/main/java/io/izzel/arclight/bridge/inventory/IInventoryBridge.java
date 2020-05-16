package io.izzel.arclight.bridge.inventory;

import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_14_R1.entity.CraftHumanEntity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.InventoryHolder;

import java.util.List;

public interface IInventoryBridge {

    int MAX_STACK = 64;

    List<ItemStack> bridge$getContents();

    void bridge$onOpen(CraftHumanEntity who);

    void bridge$onClose(CraftHumanEntity who);

    List<HumanEntity> bridge$getViewers();

    InventoryHolder bridge$getOwner();

    void bridge$setOwner(InventoryHolder owner);

    void bridge$setMaxStackSize(int size);

    Location bridge$getLocation();

    IRecipe<?> bridge$getCurrentRecipe();

    void bridge$setCurrentRecipe(IRecipe<?> recipe);
}
