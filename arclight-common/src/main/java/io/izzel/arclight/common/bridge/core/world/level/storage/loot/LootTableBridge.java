package io.izzel.arclight.common.bridge.core.world.level.storage.loot;

import org.bukkit.craftbukkit.CraftLootTable;

public interface LootTableBridge {

    void bridge$setCraftLootTable(CraftLootTable lootTable);

    CraftLootTable bridge$getCraftLootTable();
}
