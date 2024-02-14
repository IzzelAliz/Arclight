package io.izzel.arclight.common.bridge.core.entity.item;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public interface ItemEntityBridge {

    default int bridge$forge$onItemPickup(Player player) {
        return 0;
    }

    default void bridge$forge$firePlayerItemPickupEvent(Player player, ItemStack clone) {}

    default void bridge$forge$optimization$discardItemEntity() {}

    boolean bridge$common$itemDespawnEvent();
}
