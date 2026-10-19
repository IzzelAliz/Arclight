package io.izzel.arclight.common.bridge.core.world.inventory;

import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.bukkit.craftbukkit.entity.CraftHumanEntity;
import org.bukkit.inventory.InventoryView;

public interface AbstractContainerMenuBridge {

    void bridge$startOpen();

    InventoryView bridge$getBukkitView();

    void bridge$transferTo(AbstractContainerMenu other, CraftHumanEntity player);

    Component bridge$getTitle();

    void bridge$setTitle(Component title);

    boolean bridge$isCheckReachable();
}
