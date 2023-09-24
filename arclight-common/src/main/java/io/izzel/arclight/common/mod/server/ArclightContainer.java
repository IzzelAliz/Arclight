package io.izzel.arclight.common.mod.server;

import io.izzel.arclight.common.bridge.core.entity.player.PlayerEntityBridge;
import io.izzel.arclight.common.bridge.core.inventory.IInventoryBridge;
import io.izzel.arclight.common.bridge.core.inventory.container.PosContainerBridge;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
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
    public static InventoryView createInvView(AbstractContainerMenu container) {
        var containerOwner = ArclightCaptures.getContainerOwner();
        Inventory viewing = createInv(containerOwner, container);
        return new CraftInventoryView(((PlayerEntityBridge) containerOwner).bridge$getBukkitEntity(), viewing, container);
    }

    public static CraftInventory createInv(Player containerOwner, AbstractContainerMenu container) {
        return new CraftInventory(new ContainerInvWrapper(container, containerOwner));
    }

    public static SimpleContainer copyOf(SimpleContainer container) {
        var copy = new SimpleContainer(container.getContainerSize());
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            copy.items.set(slot, container.items.get(slot).copy());
        }
        return copy;
    }

    private static class ContainerInvWrapper implements Container, IInventoryBridge {

        private final AbstractContainerMenu container;
        private InventoryHolder owner;
        private final List<HumanEntity> viewers = new ArrayList<>();

        public ContainerInvWrapper(AbstractContainerMenu container, Player owner) {
            this.container = container;
            this.owner = ((PlayerEntityBridge) owner).bridge$getBukkitEntity();
        }

        @Override
        public int getContainerSize() {
            return this.container.lastSlots.size();
        }

        @Override
        public boolean isEmpty() {
            for (Slot slot : container.slots) {
                if (!slot.getItem().isEmpty()) return false;
            }
            return true;
        }

        @Override
        public @NotNull ItemStack getItem(int index) {
            if (index >= getContainerSize()) return ItemStack.EMPTY;
            return container.getSlot(index).getItem();
        }

        @Override
        public @NotNull ItemStack removeItem(int index, int count) {
            if (index >= getContainerSize()) return ItemStack.EMPTY;
            return container.getSlot(index).remove(count);
        }

        @Override
        public @NotNull ItemStack removeItemNoUpdate(int index) {
            if (index >= getContainerSize()) return ItemStack.EMPTY;
            return container.getSlot(index).remove(Integer.MAX_VALUE);
        }

        @Override
        public void setItem(int index, @NotNull ItemStack stack) {
            if (index >= getContainerSize()) return;
            container.getSlot(index).set(stack);
        }

        @Override
        public int getMaxStackSize() {
            if (getContainerSize() <= 0) return 0;
            return container.getSlot(0).getMaxStackSize();
        }

        @Override
        public void setChanged() {
        }

        @Override
        public boolean stillValid(@NotNull Player player) {
            return this.container.stillValid(player);
        }

        @Override
        public void clearContent() {
            for (Slot slot : this.container.slots) {
                slot.remove(Integer.MAX_VALUE);
            }
        }

        @Override
        public List<ItemStack> getContents() {
            container.broadcastChanges();
            return container.lastSlots.subList(0, getContainerSize());
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
        public RecipeHolder<?> getCurrentRecipe() {
            return null;
        }

        @Override
        public void setCurrentRecipe(RecipeHolder<?> recipe) {
        }
    }
}
