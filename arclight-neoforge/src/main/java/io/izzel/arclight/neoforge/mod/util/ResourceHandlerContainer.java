package io.izzel.arclight.neoforge.mod.util;

import io.izzel.arclight.common.bridge.core.world.IInventoryBridge;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.bukkit.Location;
import org.bukkit.craftbukkit.entity.CraftHumanEntity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.InventoryHolder;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * Read-only {@link Container} view of a NeoForge {@link ResourceHandler} for Bukkit inventory events.
 */
public class ResourceHandlerContainer implements Container, IInventoryBridge {

    @Nonnull
    private final ResourceHandler<ItemResource> delegate;
    private final List<HumanEntity> transaction = new ArrayList<>();

    public ResourceHandlerContainer(@Nonnull ResourceHandler<ItemResource> delegate) {
        this.delegate = delegate;
    }

    public ResourceHandler<ItemResource> handler() {
        return delegate;
    }

    @Override
    public int getContainerSize() {
        return delegate.size();
    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < delegate.size(); i++) {
            if (!delegate.getResource(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int index) {
        if (index < 0 || index >= delegate.size()) {
            return ItemStack.EMPTY;
        }
        return delegate.getResource(index).toStack();
    }

    @Override
    public ItemStack removeItem(int index, int amount) {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItemNoUpdate(int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public void setItem(int index, ItemStack stack) {
    }

    @Override
    public void setChanged() {
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void clearContent() {
    }

    @Override
    public void onOpen(CraftHumanEntity who) {
        transaction.add(who);
    }

    @Override
    public void onClose(CraftHumanEntity who) {
        transaction.remove(who);
    }

    @Override
    public List<HumanEntity> getViewers() {
        return transaction;
    }

    @Override
    public InventoryHolder getOwner() {
        return null;
    }

    @Override
    public void setOwner(InventoryHolder owner) {
    }

    @Override
    public void setMaxStackSize(int size) {
    }

    @Override
    public Location getLocation() {
        return null;
    }
}
