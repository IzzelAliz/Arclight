package io.izzel.arclight.neoforge.mod.util;

import io.izzel.arclight.common.bridge.core.world.IInventoryBridge;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v.entity.CraftHumanEntity;
import org.bukkit.craftbukkit.v.inventory.CraftInventory;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * Bukkit's generic inventory view for a transactional NeoForge item handler.
 *
 * <p>This deliberately does not attempt to unwrap a {@link ResourceHandler} as an old
 * IItemHandler/Container. Native container-backed handlers take the vanilla Container branch
 * before the ResourceHandler hopper hook, while an arbitrary mod handler has no owner Container
 * to unwrap. Mutating operations commit only a complete operation. They use a root transaction
 * only when no transaction is active; a listener which re-enters a transactional handler operation
 * receives a child transaction instead of triggering NeoForge's nested-root rejection.</p>
 */
public final class ResourceHandlerContainer implements Container, IInventoryBridge {

    public static Inventory getOwnerInventory(ResourceHandler<ItemResource> handler) {
        return new CraftInventory(new ResourceHandlerContainer(handler));
    }

    @Nonnull
    private final ResourceHandler<ItemResource> delegate;
    private final List<HumanEntity> transaction = new ArrayList<>();

    public ResourceHandlerContainer(@Nonnull ResourceHandler<ItemResource> delegate) {
        this.delegate = delegate;
    }

    public ResourceHandler<ItemResource> getHandler() {
        return delegate;
    }

    @Override
    public int getContainerSize() {
        return delegate.size();
    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < delegate.size(); i++) {
            if (!delegate.getResource(i).isEmpty() && delegate.getAmountAsLong(i) > 0) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int i) {
        ItemResource resource = delegate.getResource(i);
        return resource.isEmpty() ? ItemStack.EMPTY : resource.toStack(delegate.getAmountAsInt(i));
    }

    @Override
    public ItemStack removeItem(int i, int amount) {
        ItemResource resource = delegate.getResource(i);
        if (resource.isEmpty() || amount <= 0) {
            return ItemStack.EMPTY;
        }
        int requested = Math.min(amount, delegate.getAmountAsInt(i));
        try (var transaction = arclight$openTransaction()) {
            int extracted = delegate.extract(i, resource, requested, transaction);
            if (extracted == 0) {
                return ItemStack.EMPTY;
            }
            transaction.commit();
            return resource.toStack(extracted);
        }
    }

    @Override
    public ItemStack removeItemNoUpdate(int i) {
        return removeItem(i, Integer.MAX_VALUE);
    }

    @Override
    public void setItem(int i, ItemStack stack) {
        ItemResource current = delegate.getResource(i);
        int currentAmount = current.isEmpty() ? 0 : delegate.getAmountAsInt(i);
        ItemResource replacement = ItemResource.of(stack);
        int replacementAmount = stack.isEmpty() ? 0 : stack.getCount();
        if (replacementAmount > 0 && !delegate.isValid(i, replacement)) {
            return;
        }
        try (var transaction = arclight$openTransaction()) {
            if (currentAmount > 0 && delegate.extract(i, current, currentAmount, transaction) != currentAmount) {
                return;
            }
            if (replacementAmount > 0 && delegate.insert(i, replacement, replacementAmount, transaction) != replacementAmount) {
                return;
            }
            transaction.commit();
        }
    }

    private Transaction arclight$openTransaction() {
        TransactionContext current = Transaction.getCurrentOpenedTransaction();
        return current == null ? Transaction.openRoot() : Transaction.open(current);
    }

    @Override
    public int getMaxStackSize() {
        int result = 0;
        for (int i = 0; i < delegate.size(); i++) {
            ItemResource resource = delegate.getResource(i);
            result = Math.max(result, (int) Math.min(Integer.MAX_VALUE, delegate.getCapacityAsLong(i, resource)));
        }
        return result == 0 ? MAX_STACK : result;
    }

    @Override
    public void setChanged() {
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public boolean canPlaceItem(int i, ItemStack stack) {
        return !stack.isEmpty() && delegate.isValid(i, ItemResource.of(stack));
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < delegate.size(); i++) {
            setItem(i, ItemStack.EMPTY);
        }
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
