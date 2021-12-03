package io.izzel.arclight.common.mixin.core.world.spawner;

import io.izzel.arclight.common.bridge.core.world.spawner.WorldEntitySpawnerBridge;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LocalMobCapCalculator;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(NaturalSpawner.SpawnState.class)
public abstract class WorldEntitySpawner_EntityDensityManagerMixin implements WorldEntitySpawnerBridge.EntityDensityManagerBridge {

    // @formatter:off
    @Shadow protected abstract void afterSpawn(Mob p_234990_1_, ChunkAccess p_234990_2_);
    @Shadow @Final private int spawnableChunkCount;
    @Shadow @Final private Object2IntOpenHashMap<MobCategory> mobCategoryCounts;
    @Shadow protected abstract boolean canSpawn(EntityType<?> p_234989_1_, BlockPos p_234989_2_, ChunkAccess p_234989_3_);
    @Shadow @Final private LocalMobCapCalculator localMobCapCalculator;
    // @formatter:on

    @Override
    public boolean bridge$canSpawn(EntityType<?> entityType, BlockPos pos, ChunkAccess chunk) {
        return this.canSpawn(entityType, pos, chunk);
    }

    @Override
    public void bridge$updateDensity(Mob mobEntity, ChunkAccess chunk) {
        this.afterSpawn(mobEntity, chunk);
    }

    @Override
    public boolean bridge$canSpawn(MobCategory classification, ChunkPos pos, int limit) {
        int i = limit * this.spawnableChunkCount / 289;
        return this.mobCategoryCounts.getInt(classification) >= i ? false : this.localMobCapCalculator.canSpawn(classification, pos);
    }
}
