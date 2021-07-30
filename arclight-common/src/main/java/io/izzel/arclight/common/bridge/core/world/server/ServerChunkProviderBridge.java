package io.izzel.arclight.common.bridge.core.world.server;

import java.io.IOException;
import net.minecraft.server.level.ThreadedLevelLightEngine;
import net.minecraft.world.level.chunk.ChunkGenerator;

public interface ServerChunkProviderBridge {

    void bridge$close(boolean save) throws IOException;

    void bridge$purgeUnload();

    boolean bridge$tickDistanceManager();

    boolean bridge$isChunkLoaded(int x, int z);

    ThreadedLevelLightEngine bridge$getLightManager();

    void bridge$setChunkGenerator(ChunkGenerator chunkGenerator);

    void bridge$setViewDistance(int viewDistance);
}
