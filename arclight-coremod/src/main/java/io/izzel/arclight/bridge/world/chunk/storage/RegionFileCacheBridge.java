package io.izzel.arclight.bridge.world.chunk.storage;

import net.minecraft.util.math.ChunkPos;

import java.io.IOException;

public interface RegionFileCacheBridge {

    boolean bridge$chunkExists(ChunkPos pos) throws IOException;
}
