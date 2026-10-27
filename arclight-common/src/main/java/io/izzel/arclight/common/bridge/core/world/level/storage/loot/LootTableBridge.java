package io.izzel.arclight.common.bridge.core.world.level.storage.loot;

import org.bukkit.craftbukkit.CraftLootTable;

public interface LootTableBridge {

    void bridge$setCraftLootTable(CraftLootTable lootTable);

    CraftLootTable bridge$getCraftLootTable();

    void bridge$fillInventory(net.minecraft.world.Container inventory, net.minecraft.world.level.storage.loot.LootParams params,
                              @org.jetbrains.annotations.Nullable net.minecraft.util.RandomSource random, boolean plugin);
}
