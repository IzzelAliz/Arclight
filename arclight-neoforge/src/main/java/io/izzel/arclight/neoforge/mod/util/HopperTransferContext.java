package io.izzel.arclight.neoforge.mod.util;

import io.izzel.arclight.common.bridge.core.world.IInventoryBridge;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ContainerOrHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import org.bukkit.craftbukkit.inventory.CraftInventory;
import org.bukkit.inventory.Inventory;

public final class HopperTransferContext {

    private static final ThreadLocal<ContainerOrHandler> CURRENT = new ThreadLocal<>();

    private HopperTransferContext() {
    }

    public static void push(ContainerOrHandler value) {
        CURRENT.set(value);
    }

    public static void clear() {
        CURRENT.remove();
    }

    public static ContainerOrHandler peek() {
        return CURRENT.get();
    }

    public static Inventory toInventory(ContainerOrHandler containerOrHandler) {
        if (containerOrHandler == null || containerOrHandler.isEmpty()) {
            return null;
        }
        Container container = containerOrHandler.container();
        if (container != null) {
            if (container instanceof CompoundContainer compoundContainer) {
                return new CraftInventory(compoundContainer);
            }
            return ((IInventoryBridge) container).getOwnerInventory();
        }
        ResourceHandler<ItemResource> handler = containerOrHandler.itemHandler();
        if (handler != null) {
            return new CraftInventory(new ResourceHandlerContainer(handler));
        }
        return null;
    }
}
