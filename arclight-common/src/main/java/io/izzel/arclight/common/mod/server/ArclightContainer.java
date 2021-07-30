package io.izzel.arclight.common.mod.server;

import io.izzel.arclight.api.Unsafe;
import io.izzel.arclight.common.bridge.core.entity.player.PlayerEntityBridge;
import io.izzel.arclight.common.bridge.core.inventory.IInventoryBridge;
import io.izzel.arclight.common.bridge.core.inventory.container.PosContainerBridge;
import io.izzel.arclight.common.mod.ArclightMod;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import io.izzel.tools.product.Product;
import io.izzel.tools.product.Product2;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
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

    private static Container getActualInventoryForSlot(Slot slot) {
        if (slot instanceof SlotItemHandler) {
            return getInventoryFromWrapper(((SlotItemHandler) slot).getItemHandler());
        } else {
            return slot.container;
        }
    }

    private static Container getInventoryFromWrapper(IItemHandler handler) {
        if (handler instanceof CombinedInvWrapper) {
            IItemHandlerModifiable[] handlers = ((IItemHandlerModifiable[]) Unsafe.getObject(handler, HANDLERS_OFFSET));
            Container last = null;
            for (IItemHandlerModifiable modifiable : handlers) {
                Container inventory = getInventoryFromWrapper(modifiable);
                if (inventory instanceof net.minecraft.world.entity.player.Inventory) {
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

    public static InventoryView createInvView(AbstractContainerMenu container) {
        Product2<Player, Integer> containerInfo = getContainerInfo(container);
        Inventory viewing = new CraftInventory(new ContainerInvWrapper(container, containerInfo._2, containerInfo._1));
        return new CraftInventoryView(((PlayerEntityBridge) containerInfo._1).bridge$getBukkitEntity(), viewing, container);
    }

    public static void updateView(AbstractContainerMenu container, InventoryView inventoryView) {
        Inventory topInventory = inventoryView.getTopInventory();
        if (topInventory instanceof CraftInventory) {
            Container inventory = ((CraftInventory) topInventory).getInventory();
            if (inventory instanceof ContainerInvWrapper) {
                Product2<Player, Integer> containerInfo = getContainerInfo(container);
                ((ContainerInvWrapper) inventory).setOwner(((PlayerEntityBridge) containerInfo._1).bridge$getBukkitEntity());
                ((ContainerInvWrapper) inventory).setSize(containerInfo._2);
            }
        }
    }

    // todo check this
    private static Product2<Player, Integer> getContainerInfo(AbstractContainerMenu container) {
        Player candidate = ArclightCaptures.getContainerOwner();
        int bottomBegin = -1, bottomEnd = -1;
        for (ListIterator<Slot> iterator = container.slots.listIterator(); iterator.hasNext(); ) {
            Slot slot = iterator.next();
            Container inventory = getActualInventoryForSlot(slot);
            if (inventory instanceof net.minecraft.world.entity.player.Inventory) {
                if (candidate != null && ((net.minecraft.world.entity.player.Inventory) inventory).player != candidate) {
                    ArclightMod.LOGGER.warn("Multiple player found in {}/{}, previous {}, new {}", container, container.getClass(), candidate, ((net.minecraft.world.entity.player.Inventory) inventory).player);
                }
                candidate = ((net.minecraft.world.entity.player.Inventory) inventory).player;
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
            bottomBegin = container.slots.size();
        }
        return Product.of(candidate, bottomBegin);
    }

    private static class ContainerInvWrapper implements Container, IInventoryBridge {

        private final AbstractContainerMenu container;
        private int size;
        private InventoryHolder owner;
        private final List<HumanEntity> viewers = new ArrayList<>();

        public ContainerInvWrapper(AbstractContainerMenu container, int size, Player owner) {
            this.container = container;
            this.size = size;
            this.owner = ((PlayerEntityBridge) owner).bridge$getBukkitEntity();
        }

        public void setSize(int size) {
            this.size = size;
        }

        @Override
        public int getContainerSize() {
            return size;
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
            if (index >= size) return ItemStack.EMPTY;
            return container.getSlot(index).getItem();
        }

        @Override
        public @NotNull ItemStack removeItem(int index, int count) {
            if (index >= size) return ItemStack.EMPTY;
            return container.getSlot(index).remove(count);
        }

        @Override
        public @NotNull ItemStack removeItemNoUpdate(int index) {
            if (index >= size) return ItemStack.EMPTY;
            return container.getSlot(index).remove(Integer.MAX_VALUE);
        }

        @Override
        public void setItem(int index, @NotNull ItemStack stack) {
            if (index >= size) return;
            container.getSlot(index).set(stack);
        }

        @Override
        public int getMaxStackSize() {
            if (size <= 0) return 0;
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
            return container.lastSlots.subList(0, size);
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
        public Recipe<?> getCurrentRecipe() {
            return null;
        }

        @Override
        public void setCurrentRecipe(Recipe<?> recipe) {
        }
    }
}
