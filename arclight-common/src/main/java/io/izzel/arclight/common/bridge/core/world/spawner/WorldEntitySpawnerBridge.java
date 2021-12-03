package io.izzel.arclight.common.bridge.core.world.spawner;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;

public interface WorldEntitySpawnerBridge {

    interface EntityDensityManagerBridge {

        void bridge$updateDensity(Mob mobEntity, ChunkAccess chunk);

        boolean bridge$canSpawn(MobCategory classification, ChunkPos pos, int limit);

        boolean bridge$canSpawn(EntityType<?> entityType, BlockPos pos, ChunkAccess chunk);
    }
}
