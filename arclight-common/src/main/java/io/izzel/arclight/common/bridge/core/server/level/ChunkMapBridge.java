package io.izzel.arclight.common.bridge.core.server.level;

import io.izzel.arclight.common.mod.util.ArclightCallbackExecutor;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGenerator;

import java.util.function.BooleanSupplier;

public interface ChunkMapBridge {

    void bridge$tick(BooleanSupplier hasMoreTime);

    Iterable<ChunkHolder> bridge$getLoadedChunksIterable();

    void bridge$tickEntityTracker();

    void bridge$resendEntityPairingData(net.minecraft.world.entity.Entity entity);

    void bridge$sendEntitySpawnPacket(net.minecraft.world.entity.Entity entity, net.minecraft.server.level.ServerPlayer player);

    ArclightCallbackExecutor bridge$getCallbackExecutor();

    ChunkHolder bridge$chunkHolderAt(long chunkPos);

    void bridge$setViewDistance(int i);

    java.util.concurrent.CompletableFuture<java.util.Optional<net.minecraft.nbt.CompoundTag>> bridge$readChunk(ChunkPos pos);

    void bridge$setChunkGenerator(ChunkGenerator generator);
}
