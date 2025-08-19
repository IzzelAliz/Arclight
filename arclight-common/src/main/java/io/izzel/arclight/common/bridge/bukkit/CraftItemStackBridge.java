package io.izzel.arclight.common.bridge.bukkit;

import net.minecraft.world.entity.item.ItemEntity;

public interface CraftItemStackBridge {
    void arclight$setItemEntity(ItemEntity entity);
    ItemEntity arclight$getItemEntity();
}
