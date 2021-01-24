package io.izzel.arclight.common.mod.server;

import io.izzel.arclight.api.Unsafe;
import io.izzel.arclight.common.bridge.entity.player.PlayerEntityBridge;
import io.izzel.arclight.common.bridge.inventory.IInventoryBridge;
import io.izzel.arclight.common.bridge.inventory.container.PosContainerBridge;
import io.izzel.arclight.common.mod.ArclightMod;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.SlotItemHandler;
import net.minecraftforge.items.wrapper.CombinedInvWrapper;
import net.minecraftforge.items.wrapper.InvWrapper;
import net.minecraftforge.items.wrapper.RangedWrapper;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v.entity.CraftHumanEntity;
import org.bukkit.craftbukkit.v.inventory.CraftInventory;
import org.bukkit.craftbukkit.v.inventory.CraftInventoryView;
import org.bukkit.entity.HumanEntity;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

public class ArclightContainer {

    private static final long HANDLERS_OFFSET;
    private static final long COMPOSE_OFFSET;

    static {
        try {
            Unsafe.ensureClassInitialized(CombinedInvWrapper.class);
            Field itemHandler = CombinedInvWrapper.class.getDeclaredField("itemHandler");
            HANDLERS_OFFSET = Unsafe.objectFieldOffset(itemHandler);
            Unsafe.ensureClassInitialized(RangedWrapper.class);
            Field compose = RangedWrapper.class.getDeclaredField("compose");
            COMPOSE_OFFSET = Unsafe.objectFieldOffset(compose);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    private static IInventory getActualInventoryForSlot(Slot slot) {
        if (slot instanceof SlotItemHandler) {
            return getInventoryFromWrapper(((SlotItemHandler) slot).getItemHandler());
        } else {
            return slot.inventory;
        }
    }

    private static IInventory getInventoryFromWrapper(IItemHandler handler) {
        if (handler instanceof CombinedInvWrapper) {
            IItemHandlerModifiable[] handlers = ((IItemHandlerModifiable[]) Unsafe.getObject(handler, HANDLERS_OFFSET));
            IInventory last = null;
            for (IItemHandlerModifiable modifiable : handlers) {
                IInventory inventory = getInventoryFromWrapper(modifiable);
                if (inventory instanceof PlayerInventory) {
                    return inventory;
                } else {
                    last = inventory;
                }
            }
            return last;
        } else if (handler instanceof InvWrapper) {
            return ((InvWrapper) handler).getInv();
        } else if (handler instanceof RangedWrapper) {
            return getInventoryFromWrapper(((IItemHandler) Unsafe.getObject(handler, COMPOSE_OFFSET)));
        } else {
            return null;
        }
    }

    // todo check this
    public static InventoryView createInvView(Container container) {
        PlayerEntity candidate = ArclightCaptures.getContainerOwner();
        int bottomBegin = -1, bottomEnd = -1;
        for (ListIterator<Slot> iterator = container.inventorySlots.listIterator(); iterator.hasNext(); ) {
            Slot slot = iterator.next();
            IInventory inventory = getActualInventoryForSlot(slot);
            if (inventory instanceof PlayerInventory) {
                if (candidate != null && ((PlayerInventory) inventory).player != candidate) {
                    ArclightMod.LOGGER.warn("Multiple player found in {}/{}, previous {}, new {}", container, container.getClass(), candidate, ((PlayerInventory) inventory).player);
                }
                candidate = ((PlayerInventory) inventory).player;
                if (bottomBegin == -1 || bottomBegin < bottomEnd) {
                    bottomBegin = iterator.previousIndex();
                }
            } else {
                if (bottomEnd < bottomBegin) {
                    bottomEnd = iterator.previousIndex();
                }
            }
        }
        if (candidate == null) {
            throw new RuntimeException("candidate cannot be null, " + container + "/" + container.getClass());
        }
        if (bottomBegin < bottomEnd || bottomBegin == -1) {
            bottomBegin = container.inventorySlots.size();
        }
        Inventory viewing = new CraftInventory(new ContainerInvWrapper(container, bottomBegin, candidate));
        return new CraftInventoryView(((PlayerEntityBridge) candidate).bridge$getBukkitEntity(), viewing, container);
    }

    private static class ContainerInvWrapper implements IInventory, IInventoryBridge {

        private final Container container;
        private final int size;
        private InventoryHolder owner;
        private final List<HumanEntity> viewers = new ArrayList<>();

        public ContainerInvWrapper(Container container, int size, PlayerEntity owner) {
            this.container = container;
            this.size = size;
            this.owner = ((PlayerEntityBridge) owner).bridge$getBukkitEntity();
        }

        @Override
        public int getSizeInventory() {
            return size;
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
            if (index >= size) return ItemStack.EMPTY;
            return container.getSlot(index).getStack();
        }

        @Override
        public @NotNull ItemStack decrStackSize(int index, int count) {
            if (index >= size) return ItemStack.EMPTY;
            return container.getSlot(index).decrStackSize(count);
        }

        @Override
        public @NotNull ItemStack removeStackFromSlot(int index) {
            if (index >= size) return ItemStack.EMPTY;
            return container.getSlot(index).decrStackSize(Integer.MAX_VALUE);
        }

        @Override
        public void setInventorySlotContents(int index, @NotNull ItemStack stack) {
            if (index >= size) return;
            container.putStackInSlot(index, stack);
        }

        @Override
        public int getInventoryStackLimit() {
            if (size <= 0) return 0;
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
            return container.inventoryItemStacks.subList(0, size);
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
