package io.izzel.arclight.common.mixin.core.world.server;

import io.izzel.arclight.common.bridge.world.WorldBridge;
import io.izzel.arclight.common.bridge.world.server.ChunkManagerBridge;
import io.izzel.arclight.common.mod.util.ArclightCallbackExecutor;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.DimensionType;
import net.minecraft.world.server.ChunkHolder;
import net.minecraft.world.server.ChunkManager;
import net.minecraft.world.server.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

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

    @Redirect(method = "loadChunkData", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/server/ServerWorld;getDimensionKey()Lnet/minecraft/util/RegistryKey;"))
    private RegistryKey<DimensionType> arclight$useTypeKey(ServerWorld serverWorld) {
        return ((WorldBridge) serverWorld).bridge$getTypeKey();
    }

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
