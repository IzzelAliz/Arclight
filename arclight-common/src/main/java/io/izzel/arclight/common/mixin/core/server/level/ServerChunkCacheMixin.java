package io.izzel.arclight.common.mixin.core.server.level;

import io.izzel.arclight.common.bridge.core.world.level.WorldBridge;
import io.izzel.arclight.common.bridge.core.world.server.ChunkHolderBridge;
import io.izzel.arclight.common.bridge.core.server.level.ChunkMapBridge;
import io.izzel.arclight.common.bridge.core.world.server.ServerChunkProviderBridge;
import io.izzel.arclight.common.bridge.core.server.level.DistanceManagerBridge;
import io.izzel.arclight.common.bridge.core.world.spawner.WorldEntitySpawnerBridge;
import io.izzel.arclight.mixin.Decorate;
import net.minecraft.server.level.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.LevelData;
import org.bukkit.entity.SpawnCategory;
import org.bukkit.craftbukkit.v.util.CraftSpawnCategory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.*;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Mixin(ServerChunkCache.class)
public abstract class ServerChunkCacheMixin implements ServerChunkProviderBridge {

    // @formatter:off
    @Shadow public abstract void save(boolean flush);
    @Shadow @Final ThreadedLevelLightEngine lightEngine;
    @Shadow @Final public ChunkMap chunkMap;
    @Shadow @Final public ServerLevel level;
    @Shadow @Final private DistanceManager distanceManager;
    @Shadow protected abstract void clearCache();
    @Shadow @Nullable protected abstract ChunkHolder getVisibleChunkIfPresent(long chunkPosIn);
    @Invoker("runDistanceManagerUpdates") public abstract boolean bridge$tickDistanceManager();
    @Accessor("lightEngine") public abstract ThreadedLevelLightEngine bridge$getLightManager();
    // @formatter:on

    public boolean isChunkLoaded(final int chunkX, final int chunkZ) {
        //bridge$chunkHolderAt is getUpdatingChunkIfPresent
        ChunkHolder chunk = ((ChunkMapBridge) this.chunkMap).bridge$chunkHolderAt(ChunkPos.asLong(chunkX, chunkZ));
        return chunk != null && ((ChunkHolderBridge) chunk).bridge$getFullChunkNow() != null;
    }

    public LevelChunk getChunkUnchecked(int chunkX, int chunkZ) {
        ChunkHolder chunk = ((ChunkMapBridge) this.chunkMap).bridge$chunkHolderAt(ChunkPos.asLong(chunkX, chunkZ));
        if (chunk == null) {
            return null;
        }
        return ((ChunkHolderBridge) chunk).bridge$getFullChunkUnchecked();
    }

    @Override
    public boolean bridge$isChunkLoaded(int x, int z) {
        return isChunkLoaded(x, z);
    }

    @Override
    public void bridge$setViewDistance(int viewDistance) {
        ((ChunkMapBridge) this.chunkMap).bridge$setViewDistance(viewDistance);
    }

    @Override
    public void bridge$setSimulationDistance(int simDistance) {
        distanceManager.updateSimulationDistance(simDistance);
    }

    @ModifyVariable(method = "getChunkFutureMainThread", index = 4, at = @At("HEAD"), argsOnly = true)
    private boolean arclight$skipLoadIfUnloading(boolean flag, int chunkX, int chunkZ) {
        if (flag) {
            ChunkHolder chunkholder = this.getVisibleChunkIfPresent(ChunkPos.asLong(chunkX, chunkZ));
            if (chunkholder != null) {
                FullChunkStatus chunkStatus = ChunkLevel.fullStatus(((ChunkHolderBridge) chunkholder).bridge$getOldTicketLevel());
                FullChunkStatus currentStatus = ChunkLevel.fullStatus(chunkholder.getTicketLevel());
                return !chunkStatus.isOrAfter(FullChunkStatus.FULL) || currentStatus.isOrAfter(FullChunkStatus.FULL);
            } else {
                return true;
            }
        } else {
            return false;
        }
    }

    @Redirect(method = "tickChunks(Lnet/minecraft/util/profiling/ProfilerFiller;J)V", at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/world/level/gamerules/GameRules;get(Lnet/minecraft/world/level/gamerules/GameRule;)Ljava/lang/Object;"))
    private Object arclight$noPlayer(GameRules gameRules, GameRule<Boolean> key) {
        return gameRules.get(key) && !this.level.players().isEmpty();
    }

    @Redirect(method = "tickChunks(Lnet/minecraft/util/profiling/ProfilerFiller;J)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;getGameTime()J"))
    private long arclight$ticksPer(ServerLevel level) {
        long gameTime = level.getGameTime();
        long ticksPer = ((WorldBridge) level).bridge$ticksPerSpawnCategory().getLong(SpawnCategory.ANIMAL);
        return (ticksPer != 0L && gameTime % ticksPer == 0) ? 0 : 1;
    }

    @Redirect(method = "tickChunks(Lnet/minecraft/util/profiling/ProfilerFiller;J)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/NaturalSpawner;getFilteredSpawningCategories(Lnet/minecraft/world/level/NaturalSpawner$SpawnState;ZZZ)Ljava/util/List;"))
    private List<MobCategory> arclight$filteredSpawningCategories(NaturalSpawner.SpawnState state, boolean spawnFriendlies, boolean spawnEnemies, boolean spawnPersistent) {
        LevelData levelData = this.level.getLevelData();
        List<MobCategory> list = new ArrayList<>();
        for (MobCategory mobCategory : MobCategory.values()) {
            if (mobCategory == MobCategory.MISC) {
                continue;
            }
            boolean spawnThisTick = true;
            int limit = mobCategory.getMaxInstancesPerChunk();
            SpawnCategory spawnCategory = CraftSpawnCategory.toBukkit(mobCategory);
            if (CraftSpawnCategory.isValidForLimits(spawnCategory)) {
                long ticksPerSpawn = ((WorldBridge) this.level).bridge$ticksPerSpawnCategory().getLong(spawnCategory);
                spawnThisTick = ticksPerSpawn != 0 && levelData.getGameTime() % ticksPerSpawn == 0;
                limit = ((WorldBridge) this.level).bridge$getWorld().getSpawnLimit(spawnCategory);
            }
            if (!spawnThisTick || limit == 0) {
                continue;
            }
            if ((spawnFriendlies || !mobCategory.isFriendly()) && (spawnEnemies || mobCategory.isFriendly()) && (spawnPersistent || !mobCategory.isPersistent())
                && ((WorldEntitySpawnerBridge.EntityDensityManagerBridge) state).bridge$canSpawn(mobCategory, null, limit)) {
                list.add(mobCategory);
            }
        }
        return list;
    }

    public void close(boolean save) throws IOException {
        if (save) {
            this.save(true);
        }
        this.lightEngine.close();
        this.chunkMap.close();
    }

    public void purgeUnload() {
        
        ((DistanceManagerBridge) this.distanceManager).bridge$tick();
        this.bridge$tickDistanceManager();
        
        ((ChunkMapBridge) this.chunkMap).bridge$tick(() -> true);
        
        this.clearCache();
    }

    @Override
    public void bridge$close(boolean save) throws IOException {
        this.close(save);
    }

    @Override
    public void bridge$purgeUnload() {
        this.purgeUnload();
    }

    @Redirect(method = "chunkAbsent", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ChunkHolder;getTicketLevel()I"), require = 0)
    public int arclight$useOldTicketLevel(ChunkHolder chunkHolder) {
        // XXX: Disable for C2ME (#1597)
        return ((ChunkHolderBridge) chunkHolder).bridge$getOldTicketLevel();
    }
}
