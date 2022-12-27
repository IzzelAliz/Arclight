package io.izzel.arclight.common.mod.inventory;

import net.minecraft.world.Container;
import org.bukkit.entity.HumanEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public class SideViewingTracker {

    private static final Map<Container, List<HumanEntity>> VIEWERS = Collections.synchronizedMap(new WeakHashMap<>());

    public static void onOpen(Container container, HumanEntity humanEntity) {
        VIEWERS.computeIfAbsent(container, k -> new ArrayList<>()).add(humanEntity);
    }

    public static void onClose(Container container, HumanEntity humanEntity) {
        VIEWERS.computeIfAbsent(container, k -> new ArrayList<>()).remove(humanEntity);
    }

    public static List<HumanEntity> getViewers(Container container) {
        return VIEWERS.computeIfAbsent(container, k -> new ArrayList<>());
    }
}
