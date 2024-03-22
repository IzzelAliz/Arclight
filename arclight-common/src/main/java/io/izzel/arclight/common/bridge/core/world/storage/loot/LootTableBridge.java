package io.izzel.arclight.common.bridge.core.world.storage.loot;

import org.bukkit.craftbukkit.v.CraftLootTable;

public interface LootTableBridge {

    void bridge$setCraftLootTable(CraftLootTable lootTable);

    CraftLootTable bridge$getCraftLootTable();
}
