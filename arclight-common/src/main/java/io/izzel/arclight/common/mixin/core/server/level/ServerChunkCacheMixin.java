package io.izzel.arclight.common.mixin.core.server.level;

import io.izzel.arclight.common.bridge.core.world.level.WorldBridge;
import io.izzel.arclight.common.bridge.core.world.chunk.ChunkAccessBridge;
import io.izzel.arclight.common.bridge.core.world.server.ChunkHolderBridge;
import io.izzel.arclight.common.bridge.core.server.level.ChunkMapBridge;
import io.izzel.arclight.common.bridge.core.world.server.ServerChunkCacheBridge;
import io.izzel.arclight.common.bridge.core.server.level.DistanceManagerBridge;
import net.minecraft.server.level.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.storage.LevelData;
import org.bukkit.entity.SpawnCategory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.io.IOException;

@Mixin(ServerChunkCache.class)
public abstract class ServerChunkCacheMixin implements ServerChunkCacheBridge {

    // @formatter:off
    @Shadow @Final private Thread mainThread;
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
    // Paper start
    private final ca.spottedleaf.concurrentutil.map.ConcurrentLong2ReferenceChainedHashTable<net.minecraft.world.level.chunk.LevelChunk> fullChunks = new ca.spottedleaf.concurrentutil.map.ConcurrentLong2ReferenceChainedHashTable<>();
    // Paper end

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

    // Paper start
    public void bridge$addLoadedChunk(LevelChunk chunk) {
        this.fullChunks.put(((ChunkAccessBridge) chunk).bridge$getCoordinateKey(), chunk);
    }

    public void bridge$removeLoadedChunk(LevelChunk chunk) {
        this.fullChunks.remove(((ChunkAccessBridge) chunk).bridge$getCoordinateKey());
    }

    // "real" get chunk if loaded
    // Note: Partially copied from the getChunkAt method below
    @Nullable
    public LevelChunk getChunkAtIfCachedImmediately(int x, int z) {
        long k = ChunkPos.asLong(x, z);

        // Note: Bypass cache since we need to check ticket level, and to make this MT-Safe

        ChunkHolder playerChunk = this.getVisibleChunkIfPresent(k);
        if (playerChunk == null) {
            return null;
        }

        return ((ChunkHolderBridge) playerChunk).bridge$getFullChunkUnchecked();
    }

    @Nullable
    public LevelChunk bridge$getChunkAtIfLoadedImmediately(int x, int z) {
        return this.fullChunks.get(ChunkPos.asLong(x, z));
    }
    // Paper end

    @Override
    public void bridge$setViewDistance(int viewDistance) {
        ((ChunkMapBridge) this.chunkMap).bridge$setViewDistance(viewDistance);
    }

    @Override
    public void bridge$setSimulationDistance(int simDistance) {
        distanceManager.updateSimulationDistance(simDistance);
    }

    @Inject(method = "getChunk", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;getProfiler()Lnet/minecraft/util/profiling/ProfilerFiller;"), cancellable = true)
    private void optimiseGetChunkAtCallForLoadedChunks(int x, int z, ChunkStatus chunkStatus, boolean requireChunk, CallbackInfoReturnable<ChunkAccess> cir) {
        // Paper start - Perf: Optimise getChunkAt calls for loaded chunks
        LevelChunk ifLoaded = this.getChunkAtIfCachedImmediately(x, z);
        if (ifLoaded != null) {
            cir.setReturnValue(ifLoaded);
        }
        // Paper end - Perf: Optimise getChunkAt calls for loaded chunks
    }

    /**
     * @author MemencioPerez
     * @reason Optimise getChunkAt calls for loaded chunks
     */
    @Overwrite
    public @org.jetbrains.annotations.Nullable LevelChunk getChunkNow(int chunkX, int chunkZ) {
        if (Thread.currentThread() != this.mainThread) {
            return null;
        } else {
            return this.getChunkAtIfCachedImmediately(chunkX, chunkZ); // Paper - Perf: Optimise getChunkAt calls for loaded chunks
        }
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

    @Redirect(method = "tickChunks", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/GameRules;getBoolean(Lnet/minecraft/world/level/GameRules$Key;)Z"))
    private boolean arclight$noPlayer(GameRules gameRules, GameRules.Key<GameRules.BooleanValue> key) {
        return gameRules.getBoolean(key) && !this.level.players().isEmpty();
    }

    @Redirect(method = "tickChunks", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/storage/LevelData;getGameTime()J"))
    private long arclight$ticksPer(LevelData worldInfo) {
        long gameTime = worldInfo.getGameTime();
        long ticksPer = ((WorldBridge) this.level).bridge$ticksPerSpawnCategory().getLong(SpawnCategory.ANIMAL);
        return (ticksPer != 0L && gameTime % ticksPer == 0) ? 0 : 1;
    }

    public void close(boolean save) throws IOException {
        if (save) {
            this.save(true);
        }
        this.lightEngine.close();
        this.chunkMap.close();
    }

    public void purgeUnload() {
        this.level.getProfiler().push("purge");
        ((DistanceManagerBridge) this.distanceManager).bridge$tick();
        this.bridge$tickDistanceManager();
        this.level.getProfiler().popPush("unload");
        ((ChunkMapBridge) this.chunkMap).bridge$tick(() -> true);
        this.level.getProfiler().pop();
        this.clearCache();
    }

    @Redirect(method = "chunkAbsent", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ChunkHolder;getTicketLevel()I"), require = 0)
    public int arclight$useOldTicketLevel(ChunkHolder chunkHolder) {
        // XXX: Disable for C2ME (#1597)
        return ((ChunkHolderBridge) chunkHolder).bridge$getOldTicketLevel();
    }
}
