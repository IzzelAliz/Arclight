package io.izzel.arclight.common.bridge.core.world.server;

import javax.annotation.Nullable;
import net.minecraft.server.level.ThreadedLevelLightEngine;
import net.minecraft.world.level.chunk.LevelChunk;

public interface ServerChunkCacheBridge {

    void bridge$addLoadedChunk(LevelChunk chunk);

    void bridge$removeLoadedChunk(LevelChunk chunk);

    @Nullable
    LevelChunk bridge$getChunkAtIfLoadedImmediately(int x, int z);

    boolean bridge$tickDistanceManager();

    ThreadedLevelLightEngine bridge$getLightManager();

    void bridge$setViewDistance(int viewDistance);

    void bridge$setSimulationDistance(int simDistance);
}