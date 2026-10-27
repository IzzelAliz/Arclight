package io.izzel.arclight.common.bridge.core.world.inventory;

import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.bukkit.craftbukkit.entity.CraftHumanEntity;
import org.bukkit.inventory.InventoryView;

public interface AbstractContainerMenuBridge {

    default void bridge$startOpen() {
        throw new UnsupportedOperationException("Implemented by AbstractContainerMenuMixin");
    }

    default void bridge$broadcastCarriedItem() {
        throw new UnsupportedOperationException("Implemented by AbstractContainerMenuMixin");
    }

    default void bridge$forceSlot(net.minecraft.world.Container container, int slot) {
        throw new UnsupportedOperationException("Implemented by AbstractContainerMenuMixin");
    }

    default InventoryView getBukkitView() {
        throw new UnsupportedOperationException("Implemented by AbstractContainerMenuMixin");
    }

    default InventoryView bridge$getBukkitView() {
        throw new UnsupportedOperationException("Implemented by AbstractContainerMenuMixin");
    }

    default void bridge$transferTo(AbstractContainerMenu other, CraftHumanEntity player) {
        throw new UnsupportedOperationException("Implemented by AbstractContainerMenuMixin");
    }

    default Component bridge$getTitle() {
        throw new UnsupportedOperationException("Implemented by AbstractContainerMenuMixin");
    }

    default void bridge$setTitle(Component title) {
        throw new UnsupportedOperationException("Implemented by AbstractContainerMenuMixin");
    }

    default boolean bridge$isCheckReachable() {
        throw new UnsupportedOperationException("Implemented by AbstractContainerMenuMixin");
    }

    default void bridge$setCheckReachable(boolean checkReachable) {
        throw new UnsupportedOperationException("Implemented by AbstractContainerMenuMixin");
    }

    default void broadcastCarriedItem() {
        this.bridge$broadcastCarriedItem();
    }

    default void forceSlot(net.minecraft.world.Container container, int slot) {
        this.bridge$forceSlot(container, slot);
    }

    default net.minecraft.network.chat.Component getTitle() {
        return this.bridge$getTitle();
    }

    default void setTitle(net.minecraft.network.chat.Component title) {
        this.bridge$setTitle(title);
    }

    default void transferTo(net.minecraft.world.inventory.AbstractContainerMenu other, org.bukkit.craftbukkit.entity.CraftHumanEntity player) {
        this.bridge$transferTo(other, player);
    }

    default void startOpen() {
        this.bridge$startOpen();
    }

}
