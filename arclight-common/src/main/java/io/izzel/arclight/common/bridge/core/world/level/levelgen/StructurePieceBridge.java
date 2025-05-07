package io.izzel.arclight.common.bridge.core.world.level.levelgen;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.loot.LootTable;
import org.bukkit.craftbukkit.v.block.CraftBlockEntityState;

public interface StructurePieceBridge {
    void bridge$placeCraftBlockEntity(ServerLevelAccessor worldAccess, BlockPos position, CraftBlockEntityState<?> craftBlockEntityState, int i);
    void bridge$placeCraftSpawner(ServerLevelAccessor worldAccess, BlockPos position, org.bukkit.entity.EntityType entityType, int i);
    void bridge$setCraftLootTable(ServerLevelAccessor worldAccess, BlockPos position, RandomSource randomSource, ResourceKey<LootTable> loottableKey);
}
