package io.izzel.arclight.common.mod.server;

import io.izzel.arclight.common.bridge.entity.player.PlayerEntityBridge;
import io.izzel.arclight.common.bridge.inventory.IInventoryBridge;
import io.izzel.arclight.common.bridge.inventory.container.PosContainerBridge;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v.entity.CraftHumanEntity;
import org.bukkit.craftbukkit.v.inventory.CraftInventory;
import org.bukkit.craftbukkit.v.inventory.CraftInventoryView;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ArclightContainer {

    /*
     * Treat all modded containers not having a "bottom" inventory.
     */
    public static InventoryView createInvView(Container container) {
        PlayerEntity containerOwner = ArclightCaptures.getContainerOwner();
        Inventory viewing = new CraftInventory(new ContainerInvWrapper(container, containerOwner));
        return new CraftInventoryView(((PlayerEntityBridge) containerOwner).bridge$getBukkitEntity(), viewing, container);
    }

    private static class ContainerInvWrapper implements IInventory, IInventoryBridge {

        private final Container container;
        private InventoryHolder owner;
        private final List<HumanEntity> viewers = new ArrayList<>();

        public ContainerInvWrapper(Container container, PlayerEntity owner) {
            this.container = container;
            this.owner = ((PlayerEntityBridge) owner).bridge$getBukkitEntity();
        }

        @Override
        public int getSizeInventory() {
            return container.inventorySlots.size();
        }

        @Override
        public boolean isEmpty() {
            for (Slot slot : container.inventorySlots) {
                if (!slot.getStack().isEmpty()) return false;
            }
            return true;
        }

        @Override
        public @NotNull ItemStack getStackInSlot(int index) {
            if (index >= getSizeInventory()) return ItemStack.EMPTY;
            return container.getSlot(index).getStack();
        }

        @Override
        public @NotNull ItemStack decrStackSize(int index, int count) {
            if (index >= getSizeInventory()) return ItemStack.EMPTY;
            return container.getSlot(index).decrStackSize(count);
        }

        @Override
        public @NotNull ItemStack removeStackFromSlot(int index) {
            if (index >= getSizeInventory()) return ItemStack.EMPTY;
            return container.getSlot(index).decrStackSize(Integer.MAX_VALUE);
        }

        @Override
        public void setInventorySlotContents(int index, @NotNull ItemStack stack) {
            if (index >= getSizeInventory()) return;
            container.putStackInSlot(index, stack);
        }

        @Override
        public int getInventoryStackLimit() {
            if (getSizeInventory() <= 0) return 0;
            return container.getSlot(0).getSlotStackLimit();
        }

        @Override
        public void markDirty() {
        }

        @Override
        public boolean isUsableByPlayer(@NotNull PlayerEntity player) {
            return this.container.canInteractWith(player);
        }

        @Override
        public void clear() {
            for (Slot slot : this.container.inventorySlots) {
                slot.decrStackSize(Integer.MAX_VALUE);
            }
        }

        @Override
        public List<ItemStack> getContents() {
            container.detectAndSendChanges();
            return container.inventoryItemStacks;
        }

        @Override
        public void onOpen(CraftHumanEntity who) {
            viewers.add(who);
        }

        @Override
        public void onClose(CraftHumanEntity who) {
            viewers.remove(who);
        }

        @Override
        public List<HumanEntity> getViewers() {
            return viewers;
        }

        @Override
        public InventoryHolder getOwner() {
            return owner;
        }

        @Override
        public void setOwner(InventoryHolder owner) {
            this.owner = owner;
        }

        @Override
        public void setMaxStackSize(int size) {
        }

        @Override
        public Location getLocation() {
            if (container instanceof PosContainerBridge) {
                return ((PosContainerBridge) container).bridge$getWorldLocation();
            }
            return null;
        }

        @Override
        public IRecipe<?> getCurrentRecipe() {
            return null;
        }

        @Override
        public void setCurrentRecipe(IRecipe<?> recipe) {
        }
    }
}
