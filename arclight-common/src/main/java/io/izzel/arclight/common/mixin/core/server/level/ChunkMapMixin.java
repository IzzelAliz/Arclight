package io.izzel.arclight.common.mixin.core.server.level;

import io.izzel.arclight.common.bridge.core.world.WorldBridge;
import io.izzel.arclight.common.bridge.core.world.server.ChunkMapBridge;
import io.izzel.arclight.common.mod.util.ArclightCallbackExecutor;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.LevelStem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import javax.annotation.Nullable;
import java.util.function.BooleanSupplier;

@Mixin(ChunkMap.class)
public abstract class ChunkMapMixin implements ChunkMapBridge {

    // @formatter:off
    @Shadow @Nullable protected abstract ChunkHolder getUpdatingChunkIfPresent(long chunkPosIn);
    @Shadow protected abstract Iterable<ChunkHolder> getChunks();
    @Shadow protected abstract void tick();
    @Shadow @Mutable public ChunkGenerator generator;
    @Invoker("tick") public abstract void bridge$tick(BooleanSupplier hasMoreTime);
    @Invoker("setViewDistance") public abstract void bridge$setViewDistance(int i);
    // @formatter:on

    @Redirect(method = "readChunk", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;dimension()Lnet/minecraft/resources/ResourceKey;"))
    private ResourceKey<LevelStem> arclight$useTypeKey(ServerLevel serverWorld) {
        return ((WorldBridge) serverWorld).bridge$getTypeKey();
    }

    public final ArclightCallbackExecutor callbackExecutor = new ArclightCallbackExecutor();

    @Override
    public ArclightCallbackExecutor bridge$getCallbackExecutor() {
        return this.callbackExecutor;
    }

    @Override
    public ChunkHolder bridge$chunkHolderAt(long chunkPos) {
        return getUpdatingChunkIfPresent(chunkPos);
    }

    @Override
    public Iterable<ChunkHolder> bridge$getLoadedChunksIterable() {
        return this.getChunks();
    }

    @Override
    public void bridge$tickEntityTracker() {
        this.tick();
    }

    @Override
    public void bridge$setChunkGenerator(ChunkGenerator generator) {
        this.generator = generator;
    }
}
