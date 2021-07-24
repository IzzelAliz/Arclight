package io.izzel.arclight.common.bridge.world.server;

import io.izzel.arclight.common.mod.util.ArclightCallbackExecutor;
import java.util.function.BooleanSupplier;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGenerator;

public interface ChunkManagerBridge {

    void bridge$tick(BooleanSupplier hasMoreTime);

    Iterable<ChunkHolder> bridge$getLoadedChunksIterable();

    boolean bridge$isOutsideSpawningRadius(ChunkPos chunkPosIn);

    void bridge$tickEntityTracker();

    ArclightCallbackExecutor bridge$getCallbackExecutor();

    ChunkHolder bridge$chunkHolderAt(long chunkPos);

    void bridge$setViewDistance(int i);

    void bridge$setChunkGenerator(ChunkGenerator generator);
}
