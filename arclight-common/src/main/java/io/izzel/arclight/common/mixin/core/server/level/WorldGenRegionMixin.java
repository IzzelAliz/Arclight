package io.izzel.arclight.common.mixin.core.server.level;

import io.izzel.arclight.common.bridge.core.world.level.WorldBridge;
import net.minecraft.server.level.GenerationChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.StaticCache2D;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkDependencies;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.chunk.status.ChunkStep;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(WorldGenRegion.class)
public abstract class WorldGenRegionMixin implements WorldBridge {

    // @formatter:off
    @Shadow public abstract boolean addFreshEntity(Entity entityIn);
    @Shadow @Final private ServerLevel level;
    @Shadow @Final private StaticCache2D<GenerationChunkHolder> cache;
    @Shadow @Final private ChunkAccess center;
    @Shadow @Final private ChunkStep generatingStep;
    // @formatter:on

    @Override
    public @Nullable ChunkAccess bridge$getChunkIfLoadedImmediately(int chunkX, int chunkZ) {
        if (!this.cache.contains(chunkX, chunkZ)) {
            return null;
        }
        int distance = this.center.getPos().getChessboardDistance(chunkX, chunkZ);
        ChunkDependencies dependencies = this.generatingStep.directDependencies();
        if (distance < 0 || distance >= dependencies.size()) {
            return null;
        }
        ChunkStatus allowed = dependencies.get(distance);
        if (!ChunkStatus.FULL.isOrBefore(allowed)) {
            return null;
        }
        GenerationChunkHolder holder = this.cache.get(chunkX, chunkZ);
        return holder == null ? null : holder.getChunkIfPresentUnchecked(allowed);
    }

    public boolean addFreshEntity(Entity entity, CreatureSpawnEvent.SpawnReason reason) {
        return this.addFreshEntity(entity);
    }

    @Override
    public boolean bridge$addEntity(Entity entity, CreatureSpawnEvent.SpawnReason reason) {
        return addFreshEntity(entity, reason);
    }

    @Override
    public void bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason reason) {
    }

    @Override
    public CraftWorld bridge$getWorld() {
        return ((WorldBridge) this.level).bridge$getWorld();
    }

    @Override
    public CreatureSpawnEvent.SpawnReason bridge$getAddEntityReason() {
        return CreatureSpawnEvent.SpawnReason.DEFAULT;
    }
}
