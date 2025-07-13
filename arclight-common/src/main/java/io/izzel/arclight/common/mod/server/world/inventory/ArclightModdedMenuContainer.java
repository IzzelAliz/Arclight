package io.izzel.arclight.common.mod.server.world.inventory;

import io.izzel.arclight.common.bridge.core.entity.player.PlayerEntityBridge;
import io.izzel.arclight.common.bridge.core.inventory.IInventoryBridge;
import io.izzel.arclight.common.bridge.core.inventory.container.PosContainerBridge;
import io.izzel.arclight.common.mod.server.ArclightServer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v.entity.CraftHumanEntity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ArclightModdedMenuContainer implements Container, IInventoryBridge {

    private final AbstractContainerMenu container;
    private InventoryHolder owner;
    private final List<HumanEntity> viewers = new ArrayList<>();

    public ArclightModdedMenuContainer(AbstractContainerMenu container, Player owner) {
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
        final var slot = container.getSlot(index);
        return slot.container.removeItemNoUpdate(slot.slot);
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
        Set<Container> containers = new HashSet<>();
        for (Slot slot : container.slots) {
            if (!containers.contains(slot.container)) {
                slot.container.setChanged();
                containers.add(slot.container);
            }
        }
        containers.clear();
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return this.container.stillValid(player);
    }

    @Override
    public void clearContent() {
        ArclightServer.LOGGER.debug("Clearing everything for a modded container menu container");
        for (Slot slot : container.slots) {
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
