package io.izzel.arclight.common.mixin.core.world.server;

import io.izzel.arclight.common.bridge.world.server.ChunkManagerBridge;
import io.izzel.arclight.common.mod.util.ArclightCallbackExecutor;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.server.ChunkHolder;
import net.minecraft.world.server.ChunkManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;

import javax.annotation.Nullable;
import java.util.function.BooleanSupplier;

@Mixin(ChunkManager.class)
public abstract class ChunkManagerMixin implements ChunkManagerBridge {

    // @formatter:off
    @Shadow @Nullable protected abstract ChunkHolder func_219220_a(long chunkPosIn);
    @Shadow protected abstract Iterable<ChunkHolder> getLoadedChunksIterable();
    @Shadow abstract boolean isOutsideSpawningRadius(ChunkPos chunkPosIn);
    @Shadow protected abstract void tickEntityTracker();
    @Invoker("tick") public abstract void bridge$tick(BooleanSupplier hasMoreTime);
    @Invoker("setViewDistance") public abstract void bridge$setViewDistance(int i);
    // @formatter:on

    public final ArclightCallbackExecutor callbackExecutor = new ArclightCallbackExecutor();

    @Override
    public ArclightCallbackExecutor bridge$getCallbackExecutor() {
        return this.callbackExecutor;
    }

    @Override
    public ChunkHolder bridge$chunkHolderAt(long chunkPos) {
        return func_219220_a(chunkPos);
    }

    @Override
    public Iterable<ChunkHolder> bridge$getLoadedChunksIterable() {
        return this.getLoadedChunksIterable();
    }

    @Override
    public boolean bridge$isOutsideSpawningRadius(ChunkPos chunkPosIn) {
        return this.isOutsideSpawningRadius(chunkPosIn);
    }

    @Override
    public void bridge$tickEntityTracker() {
        this.tickEntityTracker();
    }
}
