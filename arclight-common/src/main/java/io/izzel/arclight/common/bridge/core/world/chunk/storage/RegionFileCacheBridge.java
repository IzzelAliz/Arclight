package io.izzel.arclight.common.bridge.core.world.chunk.storage;

import java.io.IOException;
import net.minecraft.world.level.ChunkPos;

public interface RegionFileCacheBridge {

    boolean bridge$chunkExists(ChunkPos pos) throws IOException;
}
