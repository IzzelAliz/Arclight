package io.izzel.arclight.mixin.core.inventory;

import io.izzel.arclight.bridge.inventory.IInventoryBridge;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.NonNullList;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_14_R1.entity.CraftHumanEntity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.InventoryHolder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.ArrayList;
import java.util.List;

@Mixin(Inventory.class)
public abstract class InventoryMixin implements IInventory, IInventoryBridge {

    // @formatter:off
    @Shadow @Final public NonNullList<ItemStack> inventoryContents;
    // @formatter:on

    private static final int MAX_STACK = 64;

    public List<HumanEntity> transaction = new ArrayList<>();
    private int maxStack = MAX_STACK;
    protected InventoryHolder bukkitOwner;

    public void arclight$constructor(int numSlots) {
        throw new RuntimeException();
    }

    public void arclight$constructor(int numSlots, InventoryHolder owner) {
        this.arclight$constructor(numSlots);
        this.bukkitOwner = owner;
    }

    @Override
    public List<ItemStack> bridge$getContents() {
        return this.inventoryContents;
    }

    @Override
    public void bridge$onOpen(CraftHumanEntity who) {
        transaction.add(who);
    }

    @Override
    public void bridge$onClose(CraftHumanEntity who) {
        transaction.remove(who);
    }

    @Override
    public List<HumanEntity> bridge$getViewers() {
        return transaction;
    }

    @Override
    public InventoryHolder bridge$getOwner() {
        return bukkitOwner;
    }

    @Override
    public void bridge$setOwner(InventoryHolder owner) {
        this.bukkitOwner = owner;
    }

    @Override
    public int getInventoryStackLimit() {
        if (maxStack == 0) maxStack = MAX_STACK;
        return maxStack;
    }

    @Override
    public void bridge$setMaxStackSize(int size) {
        this.maxStack = size;
    }

    @Override
    public Location bridge$getLocation() {
        return null;
    }

    @Override
    public IRecipe<?> bridge$getCurrentRecipe() {
        return null;
    }

    @Override
    public void bridge$setCurrentRecipe(IRecipe<?> recipe) {

    }
}
