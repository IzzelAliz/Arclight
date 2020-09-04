package io.izzel.arclight.common.mixin.core.inventory;

import io.izzel.arclight.common.bridge.entity.player.PlayerEntityBridge;
import io.izzel.arclight.common.bridge.inventory.CraftingInventoryBridge;
import io.izzel.arclight.common.bridge.inventory.IInventoryBridge;
import io.izzel.arclight.common.bridge.inventory.container.PosContainerBridge;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.NonNullList;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v.entity.CraftHumanEntity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.InventoryHolder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.ArrayList;
import java.util.List;

@Mixin(CraftingInventory.class)
public abstract class CraftingInventoryMixin implements CraftingInventoryBridge, IInventory {

    // @formatter:off
    @Shadow @Final private NonNullList<ItemStack> stackList;
    @Shadow @Final public Container eventHandler;
    // @formatter:on

    public List<HumanEntity> transaction = new ArrayList<>();
    private IRecipe<?> currentRecipe;
    public IInventory resultInventory;
    private PlayerEntity owner;
    private InventoryHolder bukkitOwner;
    private int maxStack = MAX_STACK;

    public void arclight$constructor(Container eventHandlerIn, int width, int height) {
        throw new RuntimeException();
    }

    public void arclight$constructor(Container eventHandlerIn, int width, int height, PlayerEntity owner) {
        arclight$constructor(eventHandlerIn, width, height);
        this.owner = owner;
    }

    public InventoryType getInvType() {
        return this.stackList.size() == 4 ? InventoryType.CRAFTING : InventoryType.WORKBENCH;
    }

    @Override
    public void bridge$setResultInventory(IInventory resultInventory) {
        this.resultInventory = resultInventory;
    }

    @Override
    public void bridge$setOwner(PlayerEntity owner) {
        this.owner = owner;
    }

    @Override
    public List<ItemStack> getContents() {
        return this.stackList;
    }

    @Override
    public void onOpen(CraftHumanEntity who) {
        this.transaction.add(who);
    }

    @Override
    public void onClose(CraftHumanEntity who) {
        this.transaction.remove(who);
    }

    @Override
    public List<HumanEntity> getViewers() {
        return transaction;
    }

    @Override
    public InventoryHolder getOwner() {
        if (bukkitOwner == null) {
            bukkitOwner = owner == null ? null : ((PlayerEntityBridge) owner).bridge$getBukkitEntity();
        }
        return bukkitOwner;
    }

    @Override
    public void setOwner(InventoryHolder owner) {
        this.bukkitOwner = owner;
    }

    @Override
    public int getInventoryStackLimit() {
        if (maxStack == 0) maxStack = MAX_STACK;
        return this.maxStack;
    }

    @Override
    public void setMaxStackSize(int size) {
        this.maxStack = size;
        ((IInventoryBridge) this.resultInventory).setMaxStackSize(size);
    }

    @Override
    public Location getLocation() {
        return this.eventHandler instanceof PosContainerBridge
            ? ((PosContainerBridge) eventHandler).bridge$getWorldLocation()
            : ((PlayerEntityBridge) owner).bridge$getBukkitEntity().getLocation();
    }

    @Override
    public IRecipe<?> getCurrentRecipe() {
        return this.currentRecipe;
    }

    @Override
    public void setCurrentRecipe(IRecipe<?> recipe) {
        this.currentRecipe = recipe;
    }
}
