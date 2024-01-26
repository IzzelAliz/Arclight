package io.izzel.arclight.common.bridge.core.inventory.container;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.bukkit.craftbukkit.v.entity.CraftHumanEntity;
import org.bukkit.inventory.InventoryView;

public interface ContainerBridge {

    InventoryView bridge$getBukkitView();

    void bridge$transferTo(AbstractContainerMenu other, CraftHumanEntity player);

    Component bridge$getTitle();

    void bridge$setTitle(Component title);

    boolean bridge$isCheckReachable();

    default boolean bridge$forge$onItemStackedOn(ItemStack carriedItem, ItemStack stackedOnItem, Slot slot, ClickAction action, Player player, SlotAccess carriedSlotAccess) {
        return false;
    }
}
