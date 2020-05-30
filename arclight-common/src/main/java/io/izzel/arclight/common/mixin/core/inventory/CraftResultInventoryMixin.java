package io.izzel.arclight.common.mixin.core.inventory;

import io.izzel.arclight.common.bridge.inventory.IInventoryBridge;
import net.minecraft.inventory.CraftResultInventory;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.NonNullList;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v.entity.CraftHumanEntity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.InventoryHolder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.ArrayList;
import java.util.List;

@Mixin(CraftResultInventory.class)
public abstract class CraftResultInventoryMixin implements IInventoryBridge, IInventory {

    // @formatter:off
    @Shadow @Final private NonNullList<ItemStack> stackResult;
    // @formatter:on

    private int maxStack = MAX_STACK;

    @Override
    public List<ItemStack> getContents() {
        return this.stackResult;
    }

    @Override
    public void onOpen(CraftHumanEntity who) { }

    @Override
    public void onClose(CraftHumanEntity who) { }

    @Override
    public List<HumanEntity> getViewers() {
        return new ArrayList<>();
    }

    @Override
    public InventoryHolder getOwner() { return null; }

    @Override
    public void setOwner(InventoryHolder owner) { }

    @Override
    public int getInventoryStackLimit() {
        if (maxStack == 0) maxStack = MAX_STACK;
        return this.maxStack;
    }

    @Override
    public void setMaxStackSize(int size) {
        this.maxStack = size;
    }

    @Override
    public Location getLocation() { return null; }

    @Override
    public IRecipe<?> getCurrentRecipe() { return null; }

    @Override
    public void setCurrentRecipe(IRecipe<?> recipe) { }
}
