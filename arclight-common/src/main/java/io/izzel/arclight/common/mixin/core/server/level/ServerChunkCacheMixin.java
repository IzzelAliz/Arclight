package io.izzel.arclight.common.mixin.core.server.level;

import io.izzel.arclight.common.bridge.core.world.WorldBridge;
import io.izzel.arclight.common.bridge.core.world.server.ChunkHolderBridge;
import io.izzel.arclight.common.bridge.core.world.server.ChunkMapBridge;
import io.izzel.arclight.common.bridge.core.world.server.ServerChunkProviderBridge;
import io.izzel.arclight.common.bridge.core.world.server.TicketManagerBridge;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.DistanceManager;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ThreadedLevelLightEngine;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.LevelData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

import javax.annotation.Nullable;
import java.io.IOException;

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
        ChunkHolder chunk = ((ChunkMapBridge) this.chunkMap).bridge$chunkHolderAt(ChunkPos.asLong(chunkX, chunkZ));
        return chunk != null && ((ChunkHolderBridge) chunk).bridge$getFullChunk() != null;
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
    public void bridge$setChunkGenerator(ChunkGenerator chunkGenerator) {
        ((ChunkMapBridge) this.chunkMap).bridge$setChunkGenerator(chunkGenerator);
    }

    @Override
    public void bridge$setViewDistance(int viewDistance) {
        ((ChunkMapBridge) this.chunkMap).bridge$setViewDistance(viewDistance);
    }

    @ModifyVariable(method = "getChunkFutureMainThread", index = 4, at = @At("HEAD"))
    private boolean arclight$skipIfUnloading(boolean flag, int chunkX, int chunkZ) {
        if (flag) {
            ChunkHolder chunkholder = this.getVisibleChunkIfPresent(ChunkPos.asLong(chunkX, chunkZ));
            if (chunkholder != null) {
                ChunkHolder.FullChunkStatus chunkStatus = ChunkHolder.getFullChunkStatus(((ChunkHolderBridge) chunkholder).bridge$getOldTicketLevel());
                ChunkHolder.FullChunkStatus currentStatus = ChunkHolder.getFullChunkStatus(chunkholder.getTicketLevel());
                return !chunkStatus.isOrAfter(ChunkHolder.FullChunkStatus.BORDER) || currentStatus.isOrAfter(ChunkHolder.FullChunkStatus.BORDER);
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
        long ticksPer = ((WorldBridge) this.level).bridge$ticksPerAnimalSpawns();
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
        ((TicketManagerBridge) this.distanceManager).bridge$tick();
        this.bridge$tickDistanceManager();
        this.level.getProfiler().popPush("unload");
        ((ChunkMapBridge) this.chunkMap).bridge$tick(() -> true);
        this.level.getProfiler().pop();
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

    @Redirect(method = "chunkAbsent", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ChunkHolder;getTicketLevel()I"))
    public int arclight$useOldTicketLevel(ChunkHolder chunkHolder) {
        return ((ChunkHolderBridge) chunkHolder).bridge$getOldTicketLevel();
    }
}
