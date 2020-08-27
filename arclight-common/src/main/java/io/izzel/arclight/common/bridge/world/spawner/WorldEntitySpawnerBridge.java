package io.izzel.arclight.common.bridge.world.spawner;

import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MobEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.IChunk;

public interface WorldEntitySpawnerBridge {

    interface EntityDensityManagerBridge {

        void bridge$updateDensity(MobEntity mobEntity, IChunk chunk);

        boolean bridge$canSpawn(EntityClassification classification, int limit);

        boolean bridge$canSpawn(EntityType<?> entityType, BlockPos pos, IChunk chunk);
    }
}
