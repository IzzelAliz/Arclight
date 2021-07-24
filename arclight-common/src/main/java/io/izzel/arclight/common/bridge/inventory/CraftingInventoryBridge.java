package io.izzel.arclight.common.bridge.inventory;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;

public interface CraftingInventoryBridge extends IInventoryBridge {

    void bridge$setOwner(Player owner);

    void bridge$setResultInventory(Container resultInventory);
}
