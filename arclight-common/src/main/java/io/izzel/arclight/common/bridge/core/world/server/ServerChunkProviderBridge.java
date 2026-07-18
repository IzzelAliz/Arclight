package io.izzel.arclight.common.bridge.core.world.server;

import java.io.IOException;

import javax.annotation.Nullable;

import net.minecraft.server.level.ThreadedLevelLightEngine;
import net.minecraft.world.level.chunk.LevelChunk;

public interface ServerChunkProviderBridge {

    void bridge$addLoadedChunk(LevelChunk chunk);

    void bridge$removeLoadedChunk(LevelChunk chunk);

    @Nullable
    LevelChunk bridge$getChunkAtIfLoadedImmediately(int x, int z);

    void bridge$close(boolean save) throws IOException;

    void bridge$purgeUnload();
    boolean bridge$tickDistanceManager();

    boolean bridge$isChunkLoaded(int x, int z);

    ThreadedLevelLightEngine bridge$getLightManager();

    void bridge$setViewDistance(int viewDistance);

    void bridge$setSimulationDistance(int simDistance);
}