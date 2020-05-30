package io.izzel.arclight.common.bridge.inventory;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.IInventory;

public interface CraftingInventoryBridge extends IInventoryBridge {

    void bridge$setOwner(PlayerEntity owner);

    void bridge$setResultInventory(IInventory resultInventory);
}
