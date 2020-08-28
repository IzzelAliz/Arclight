package io.izzel.arclight.common.bridge.world.server;

import io.izzel.arclight.common.mod.util.ArclightCallbackExecutor;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.server.ChunkHolder;

import java.util.function.BooleanSupplier;

public interface ChunkManagerBridge {

    void bridge$tick(BooleanSupplier hasMoreTime);

    Iterable<ChunkHolder> bridge$getLoadedChunksIterable();

    boolean bridge$isOutsideSpawningRadius(ChunkPos chunkPosIn);

    void bridge$tickEntityTracker();

    ArclightCallbackExecutor bridge$getCallbackExecutor();

    ChunkHolder bridge$chunkHolderAt(long chunkPos);

    void bridge$setViewDistance(int i);
}
