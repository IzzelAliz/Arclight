package io.izzel.arclight.common.bridge.world.server;

import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.server.ServerWorldLightManager;

import java.io.IOException;

public interface ServerChunkProviderBridge {

    void bridge$close(boolean save) throws IOException;

    void bridge$purgeUnload();

    boolean bridge$tickDistanceManager();

    boolean bridge$isChunkLoaded(int x, int z);

    ServerWorldLightManager bridge$getLightManager();

    void bridge$setChunkGenerator(ChunkGenerator chunkGenerator);

    void bridge$setViewDistance(int viewDistance);
}
