package io.izzel.arclight.mixin.core.inventory;

import io.izzel.arclight.bridge.inventory.IInventoryBridge;
import net.minecraft.inventory.CraftResultInventory;
import net.minecraft.inventory.IInventory;
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

@Mixin(CraftResultInventory.class)
public abstract class CraftResultInventoryMixin implements IInventoryBridge, IInventory {

    // @formatter:off
    @Shadow @Final private NonNullList<ItemStack> stackResult;
    // @formatter:on

    private int maxStack = MAX_STACK;

    @Override
    public List<ItemStack> bridge$getContents() {
        return this.stackResult;
    }

    @Override
    public void bridge$onOpen(CraftHumanEntity who) { }

    @Override
    public void bridge$onClose(CraftHumanEntity who) { }

    @Override
    public List<HumanEntity> bridge$getViewers() {
        return new ArrayList<>();
    }

    @Override
    public InventoryHolder bridge$getOwner() { return null; }

    @Override
    public void bridge$setOwner(InventoryHolder owner) { }

    @Override
    public int getInventoryStackLimit() {
        if (maxStack == 0) maxStack = MAX_STACK;
        return this.maxStack;
    }

    @Override
    public void bridge$setMaxStackSize(int size) {
        this.maxStack = size;
    }

    @Override
    public Location bridge$getLocation() { return null; }

    @Override
    public IRecipe<?> bridge$getCurrentRecipe() { return null; }

    @Override
    public void bridge$setCurrentRecipe(IRecipe<?> recipe) { }
}
