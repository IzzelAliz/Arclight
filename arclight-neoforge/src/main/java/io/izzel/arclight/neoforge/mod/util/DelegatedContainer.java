package io.izzel.arclight.neoforge.mod.util;

import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import io.izzel.arclight.common.bridge.core.world.IInventoryBridge;
import io.izzel.arclight.common.bridge.core.world.level.block.entity.BlockEntityBridge;
import io.izzel.arclight.common.mixin.bukkit.CraftBlockEntityStateAccessor;
import io.izzel.arclight.common.mod.server.ArclightServer;
import io.izzel.arclight.neoforge.mixin.neoforge.items.CombinedInvWrapperAccessor;
import io.izzel.arclight.neoforge.mixin.neoforge.items.RangedWrapperAccessor;
import io.izzel.arclight.neoforge.mixin.neoforge.items.SidedInvWrapperAccessor;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import net.neoforged.neoforge.items.wrapper.PlayerInvWrapper;
import net.neoforged.neoforge.items.wrapper.RangedWrapper;
import net.neoforged.neoforge.items.wrapper.SidedInvWrapper;
import org.apache.commons.lang3.tuple.Pair;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v.block.CraftBlockEntityState;
import org.bukkit.craftbukkit.v.entity.CraftEntity;
import org.bukkit.craftbukkit.v.entity.CraftHumanEntity;
import org.bukkit.craftbukkit.v.inventory.CraftBlockInventoryHolder;
import org.bukkit.craftbukkit.v.inventory.CraftInventory;
import org.bukkit.craftbukkit.v.util.CraftLocation;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class DelegatedContainer implements Container, IInventoryBridge {

    private static boolean lastItemHandlerFound = false;

    public static void recordLastHandler() {
        lastItemHandlerFound = true;
    }

    public static boolean foundLastHandler() {
        try {
            return lastItemHandlerFound;
        } finally {
            lastItemHandlerFound = false;
        }
    }

    public static Pair<IItemHandler, Object> makeItemHandlerPair(CraftInventory container) {
        if (container == null) {
            return null;
        }
        final var inventory = container.getInventory();
        if (inventory instanceof DelegatedContainer arclight) {
            return Pair.of(arclight.delegate, arclight.original);
        } else {
            final var owner = container.getHolder();
            // The switch covers all InventoryHolder implementations in CraftBukkit.
            final Object nmsOwner = switch (owner) {
                case CraftBlockEntityState<?> state ->
                        ((CraftBlockEntityStateAccessor) state).arclight$getBlockEntity();
                case CraftEntity entity -> entity.getHandle();
                case CraftBlockInventoryHolder ignored -> null; // We don't have BlockEntity for such. See ComposterBlock.
                case null, default -> null;
            };
            return Pair.of(new InvWrapper(inventory), nmsOwner);
        }
    }

    /*
     * Try to unwrap InvWrapper. This will give us the Container we want.
     */
    public static Container getContainer(IItemHandler handler) {
        return switch (handler) {
            case InvWrapper inv -> inv.getInv();
            case SidedInvWrapper sidedInv -> ((SidedInvWrapperAccessor) sidedInv).arclight$unwrap();
            case RangedWrapper ranged -> {
                handler = ((RangedWrapperAccessor) ranged).arclight$unwrap();
                yield getContainer(handler);
            }
            case PlayerInvWrapper player -> {
                handler = ((CombinedInvWrapperAccessor) player).arclight$getHandlerFromIndex(0);
                yield getContainer(handler);
            }
            case null, default -> null;
        };
    }

    public static Inventory getOwnerInventory(Object nmsOwner, IItemHandler handler) {
        Container nms = getContainer(handler);
        if (nms != null) {
            final var inventory = ((IInventoryBridge) nms).getOwnerInventory();
            if (inventory != null) {
                return inventory;
            }
        }
        return new CraftInventory(new DelegatedContainer(handler, nmsOwner));
    }

    @Nonnull
    private final IItemHandler delegate;

    @Nullable
    private final Container original;

    @Nullable
    private final Object nmsOwner;

    private final List<HumanEntity> transaction = new ArrayList<>();

    public DelegatedContainer(@Nonnull IItemHandler delegate, @Nullable Object nmsOwner) {
        this.nmsOwner = nmsOwner;
        this.delegate = delegate;
        this.original = getContainer(delegate);
    }

    public DelegatedContainer(@Nonnull Pair<IItemHandler, Object> input) {
        this(input.getLeft(), input.getRight());
    }

    @Override
    public int getContainerSize() {
        return delegate.getSlots();
    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < delegate.getSlots(); i++) {
            if (!delegate.getStackInSlot(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int i) {
        return delegate.getStackInSlot(i).copy();
    }

    @Override
    public ItemStack removeItem(int i, int j) {
        return delegate.extractItem(i, j, false);
    }

    @Override
    public ItemStack removeItemNoUpdate(int i) {
        ArclightServer.LOGGER.warn("Attempted to removeItemNoUpdate on IItemHandler, using removeItem instead");
        return delegate.extractItem(i, Integer.MAX_VALUE, false);
    }

    @Override
    public void setItem(int i, ItemStack arg) {
        if (!delegate.isItemValid(i, arg)) {
            return;
        }
        final var content = getItem(i);
        final var take = delegate.extractItem(i, Integer.MAX_VALUE, true);
        if (take.getCount() != content.getCount()) {
            return;
        }
        final var raw = delegate.extractItem(i, Integer.MAX_VALUE, false);
        if (delegate.insertItem(i, arg, true).isEmpty()) {
            delegate.insertItem(i, arg, false);
            return;
        }
        delegate.insertItem(i, raw, false);
    }

    @Override
    public int getMaxStackSize() {
        int maxStack = 0;
        for (int i = 0; i < delegate.getSlots(); i++) {
            final int limit = delegate.getSlotLimit(i);
            if (limit > maxStack) {
                maxStack = limit;
            }
        }
        return maxStack;
    }

    @Override
    public void setChanged() {
        if (original != null) {
            original.setChanged();
        } else if (nmsOwner != null) {
            if (nmsOwner instanceof BlockEntity be) {
                // Arclight: All BlockEntity implementing Container use their corresponding setChanged for Container#setChanged
                be.setChanged();
            }
        }
    }

    @Override
    public boolean stillValid(Player arg) {
        if (original != null) {
            return original.stillValid(arg);
        } else if (nmsOwner != null) {
            if (nmsOwner instanceof BlockEntity be) {
                return Container.stillValidBlockEntity(be, arg);
            } else if (nmsOwner instanceof Entity entity) {
                return arg.canInteractWithEntity(entity, 4.0F);
            }
        }
        return true;
    }

    @Override
    public boolean canPlaceItem(int i, ItemStack arg) {
        return delegate.isItemValid(i, arg);
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < delegate.getSlots(); i++) {
            delegate.extractItem(i, Integer.MAX_VALUE, false);
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
        if (original != null) {
            return ((IInventoryBridge) original).getOwner();
        } else if (nmsOwner != null) {
            if (nmsOwner instanceof BlockEntity be) {
                return ((BlockEntityBridge) be).bridge$getOwner(); // BlockEntity
            } else if (nmsOwner instanceof EntityBridge entity) {
                return entity.bridge$getBukkitEntity() instanceof InventoryHolder result ? result : null; // Entity
            }
        }
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
        if (original != null) {
            return ((IInventoryBridge) original).getLocation();
        } else if (nmsOwner != null) {
            if (nmsOwner instanceof BlockEntity be) {
                return CraftLocation.toBukkit(be.getBlockPos());
            } else if (nmsOwner instanceof Entity entity) {
                return CraftLocation.toBukkit(entity.position());
            }
        }
        return null;
    }
}
