package io.izzel.arclight.common.mod.server;

import io.izzel.arclight.api.Unsafe;
import io.izzel.arclight.common.bridge.entity.player.PlayerEntityBridge;
import io.izzel.arclight.common.mod.ArclightMod;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.CraftResultInventory;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.Slot;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.SlotItemHandler;
import net.minecraftforge.items.wrapper.CombinedInvWrapper;
import net.minecraftforge.items.wrapper.InvWrapper;
import net.minecraftforge.items.wrapper.RangedWrapper;
import org.bukkit.craftbukkit.v.inventory.CraftInventory;
import org.bukkit.craftbukkit.v.inventory.CraftInventoryCustom;
import org.bukkit.craftbukkit.v.inventory.CraftInventoryView;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

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
        PlayerEntity candidate = null;
        Set<IInventory> set = new HashSet<>();
        for (Slot slot : container.inventorySlots) {
            IInventory inventory = getActualInventoryForSlot(slot);
            if (inventory != null) {
                if (inventory instanceof PlayerInventory) {
                    if (candidate != null && ((PlayerInventory) inventory).player != candidate) {
                        ArclightMod.LOGGER.warn("Multiple player found in {}/{}, previous {}, new {}", container, container.getClass(), candidate, ((PlayerInventory) inventory).player);
                    }
                    candidate = ((PlayerInventory) inventory).player;
                }
                set.add(inventory);
            }
        }
        if (candidate == null) {
            if (ArclightCaptures.getContainerOwner() != null) {
                candidate = ArclightCaptures.getContainerOwner();
            } else {
                throw new RuntimeException("candidate cannot be null, " + container + "/" + container.getClass());
            }
        }
        CraftResultInventory resultCandidate = null;
        IInventory mainCandidate = null;
        for (IInventory inventory : set) {
            if (inventory instanceof CraftResultInventory) {
                resultCandidate = (CraftResultInventory) inventory;
            } else {
                mainCandidate = inventory;
            }
        }
        Inventory inv;
        if (mainCandidate == null && resultCandidate != null) {
            mainCandidate = resultCandidate;
            resultCandidate = null;
        }
        if (mainCandidate != null) {
            if (resultCandidate != null) {
                inv = new org.bukkit.craftbukkit.v.inventory.CraftResultInventory(mainCandidate, resultCandidate);
            } else {
                inv = new CraftInventory(mainCandidate);
            }
        } else { // container has no slots
            inv = new CraftInventoryCustom(((PlayerEntityBridge) candidate).bridge$getBukkitEntity(), 0);
        }
        return new CraftInventoryView(((PlayerEntityBridge) candidate).bridge$getBukkitEntity(), inv, container);
    }
}
