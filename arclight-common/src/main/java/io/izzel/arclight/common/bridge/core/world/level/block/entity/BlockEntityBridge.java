package io.izzel.arclight.common.bridge.core.world.level.block.entity;

import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import org.bukkit.inventory.InventoryHolder;

public interface BlockEntityBridge {

    InventoryHolder bridge$getOwner();
}
