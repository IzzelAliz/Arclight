package io.izzel.arclight.common.bridge.entity.player;

import net.minecraft.world.item.ItemStack;

public interface PlayerInventoryBridge {

    int bridge$canHold(ItemStack stack);
}
