package io.izzel.arclight.common.mixin.core.world.inventory;

import io.izzel.arclight.common.bridge.core.entity.player.PlayerEntityBridge;
import io.izzel.arclight.common.bridge.core.inventory.CraftingInventoryBridge;
import io.izzel.arclight.common.bridge.core.inventory.IInventoryBridge;
import io.izzel.arclight.common.bridge.core.inventory.container.PosContainerBridge;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
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

@Mixin(CraftingContainer.class)
public abstract class CraftingInventoryMixin implements CraftingInventoryBridge, Container {

    // @formatter:off
    @Shadow @Final private NonNullList<ItemStack> items;
    @Shadow @Final public AbstractContainerMenu menu;
    // @formatter:on

    public List<HumanEntity> transaction = new ArrayList<>();
    private Recipe<?> currentRecipe;
    public Container resultInventory;
    private Player owner;
    private InventoryHolder bukkitOwner;
    private int maxStack = MAX_STACK;

    public void arclight$constructor(AbstractContainerMenu eventHandlerIn, int width, int height) {
        throw new RuntimeException();
    }

    public void arclight$constructor(AbstractContainerMenu eventHandlerIn, int width, int height, Player owner) {
        arclight$constructor(eventHandlerIn, width, height);
        this.owner = owner;
    }

    public InventoryType getInvType() {
        return this.items.size() == 4 ? InventoryType.CRAFTING : InventoryType.WORKBENCH;
    }

    @Override
    public void bridge$setResultInventory(Container resultInventory) {
        this.resultInventory = resultInventory;
    }

    @Override
    public void bridge$setOwner(Player owner) {
        this.owner = owner;
    }

    @Override
    public List<ItemStack> getContents() {
        return this.items;
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
    public int getMaxStackSize() {
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
        return this.menu instanceof PosContainerBridge
            ? ((PosContainerBridge) menu).bridge$getWorldLocation()
            : ((PlayerEntityBridge) owner).bridge$getBukkitEntity().getLocation();
    }

    @Override
    public Recipe<?> getCurrentRecipe() {
        return this.currentRecipe;
    }

    @Override
    public void setCurrentRecipe(Recipe<?> recipe) {
        this.currentRecipe = recipe;
    }
}
