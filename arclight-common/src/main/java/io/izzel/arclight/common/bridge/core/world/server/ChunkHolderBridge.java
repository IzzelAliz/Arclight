package io.izzel.arclight.common.bridge.core.world.server;

import net.minecraft.server.level.ChunkMap;
import net.minecraft.world.level.chunk.LevelChunk;

public interface ChunkHolderBridge {

    int bridge$getOldTicketLevel();

    LevelChunk bridge$getFullChunkNow();

    LevelChunk bridge$getFullChunkUnchecked();

    void bridge$callEventIfUnloading(ChunkMap manager);
}
