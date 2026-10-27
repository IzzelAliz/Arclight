package io.izzel.arclight.common.bridge.core.world.level.block.entity;

import org.bukkit.inventory.InventoryHolder;

public interface BlockEntityBridge {

    InventoryHolder bridge$getOwner();

    java.util.Set<net.minecraft.core.component.DataComponentType<?>> applyComponentsSet(
        net.minecraft.core.component.DataComponentMap prototype, net.minecraft.core.component.DataComponentPatch patch);

    org.bukkit.craftbukkit.persistence.CraftPersistentDataContainer bridge$getPersistentDataContainer();
}
