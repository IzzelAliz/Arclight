package io.izzel.arclight.common.bridge.world.server;

import net.minecraft.world.chunk.Chunk;

public interface ChunkHolderBridge {

    int bridge$getOldTicketLevel();

    Chunk bridge$getFullChunk();
}
