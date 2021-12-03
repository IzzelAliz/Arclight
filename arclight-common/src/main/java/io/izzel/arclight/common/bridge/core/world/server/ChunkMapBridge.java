package io.izzel.arclight.common.bridge.core.world.server;

import io.izzel.arclight.common.mod.util.ArclightCallbackExecutor;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGenerator;

import java.util.function.BooleanSupplier;

public interface ChunkMapBridge {

    void bridge$tick(BooleanSupplier hasMoreTime);

    Iterable<ChunkHolder> bridge$getLoadedChunksIterable();

    void bridge$tickEntityTracker();

    ArclightCallbackExecutor bridge$getCallbackExecutor();

    ChunkHolder bridge$chunkHolderAt(long chunkPos);

    void bridge$setViewDistance(int i);

    void bridge$setChunkGenerator(ChunkGenerator generator);
}
