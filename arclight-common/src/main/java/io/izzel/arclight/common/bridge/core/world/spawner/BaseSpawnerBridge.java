package io.izzel.arclight.common.bridge.core.world.spawner;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.SpawnData;
import org.jetbrains.annotations.Nullable;

public interface BaseSpawnerBridge {

    boolean bridge$forge$checkSpawnRules(Mob mob, ServerLevelAccessor level, MobSpawnType spawnType, SpawnData spawnData, boolean result);

    void bridge$forge$finalizeSpawnerSpawn(Mob mob, ServerLevelAccessor level, DifficultyInstance difficulty, @Nullable SpawnGroupData spawnData, @Nullable CompoundTag spawnTag);
}
