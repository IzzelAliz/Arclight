package io.izzel.arclight.common.mod.inventory;

import net.minecraft.inventory.IInventory;
import org.bukkit.entity.HumanEntity;

import java.util.*;

public class SideViewingTracker {

    private static final Map<IInventory, List<HumanEntity>> VIEWERS = Collections.synchronizedMap(new WeakHashMap<>());

    public static void onOpen(IInventory IInventory, HumanEntity humanEntity) {
        VIEWERS.computeIfAbsent(IInventory, k -> new ArrayList<>()).add(humanEntity);
    }

    public static void onClose(IInventory IInventory, HumanEntity humanEntity) {
        VIEWERS.computeIfAbsent(IInventory, k -> new ArrayList<>()).remove(humanEntity);
    }

    public static List<HumanEntity> getViewers(IInventory IInventory) {
        return VIEWERS.computeIfAbsent(IInventory, k -> new ArrayList<>());
    }
}
