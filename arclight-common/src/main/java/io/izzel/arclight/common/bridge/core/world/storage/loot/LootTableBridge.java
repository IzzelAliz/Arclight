package io.izzel.arclight.common.bridge.core.world.storage.loot;

import net.minecraft.world.Container;
import net.minecraft.world.level.storage.loot.LootContext;

public interface LootTableBridge {

    void bridge$fillInventory(Container inv, LootContext context, boolean plugin);
}
