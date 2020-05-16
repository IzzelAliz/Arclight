package io.izzel.arclight.mixin.core.world.server;

import com.mojang.datafixers.util.Either;
import io.izzel.arclight.bridge.world.chunk.ChunkBridge;
import io.izzel.arclight.bridge.world.server.ChunkHolderBridge;
import io.izzel.arclight.bridge.world.server.ChunkManagerBridge;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.server.ChunkHolder;
import net.minecraft.world.server.ChunkManager;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.concurrent.CompletableFuture;

@Mixin(ChunkHolder.class)
public abstract class ChunkHolderMixin implements ChunkHolderBridge {

    // @formatter:off
    @Shadow public int field_219316_k;
    @Shadow public abstract CompletableFuture<Either<IChunk, ChunkHolder.IChunkLoadingError>> func_219301_a(ChunkStatus p_219301_1_);
    @Override @Accessor("field_219316_k") public abstract int bridge$getOldTicketLevel();
    // @formatter:on

    public Chunk getFullChunk() {
        if (!ChunkHolder.func_219286_c(this.field_219316_k).func_219065_a(ChunkHolder.LocationType.BORDER)) {
            return null; // note: using oldTicketLevel for isLoaded checks
        }
        CompletableFuture<Either<IChunk, ChunkHolder.IChunkLoadingError>> statusFuture = this.func_219301_a(ChunkStatus.FULL);
        Either<IChunk, ChunkHolder.IChunkLoadingError> either = statusFuture.getNow(null);
        return either == null ? null : (Chunk) either.left().orElse(null);
    }

    @Override
    public Chunk bridge$getFullChunk() {
        return this.getFullChunk();
    }

    @Inject(method = "func_219291_a", at = @At(value = "JUMP", opcode = Opcodes.IFEQ, ordinal = 0),
        locals = LocalCapture.CAPTURE_FAILHARD)
    public void arclight$onChunkUnload(ChunkManager chunkManager, CallbackInfo ci, ChunkStatus chunkStatus,
                                       ChunkStatus chunkStatus1, boolean flag, boolean flag1,
                                       ChunkHolder.LocationType locationType, ChunkHolder.LocationType locationType1) {
        if (locationType.func_219065_a(ChunkHolder.LocationType.BORDER) && !locationType1.func_219065_a(ChunkHolder.LocationType.BORDER)) {
            this.func_219301_a(ChunkStatus.FULL).thenAcceptAsync((either) -> {
                either.ifLeft((chunkAccess) -> {
                    Chunk chunk = (Chunk) chunkAccess;
                    // Minecraft will apply the chunks tick lists to the world once the chunk got loaded, and then store the tick
                    // lists again inside the chunk once the chunk becomes inaccessible and set the chunk's needsSaving flag.
                    // These actions may however happen deferred, so we manually set the needsSaving flag already here.
                    chunk.setModified(true);
                    ((ChunkBridge) chunk).bridge$unloadCallback();
                });
            }, ((ChunkManagerBridge) chunkManager).bridge$getCallbackExecutor());

            // Run callback right away if the future was already done
            ((ChunkManagerBridge) chunkManager).bridge$getCallbackExecutor().run();
        }
    }

    @Inject(method = "func_219291_a", at = @At("RETURN"), locals = LocalCapture.CAPTURE_FAILHARD)
    public void arclight$onChunkLoad(ChunkManager chunkManager, CallbackInfo ci, ChunkStatus chunkStatus,
                                     ChunkStatus chunkStatus1, boolean flag, boolean flag1,
                                     ChunkHolder.LocationType locationType, ChunkHolder.LocationType locationType1) {
        if (!locationType.func_219065_a(ChunkHolder.LocationType.BORDER) && locationType1.func_219065_a(ChunkHolder.LocationType.BORDER)) {
            this.func_219301_a(ChunkStatus.FULL).thenAcceptAsync((either) -> {
                either.ifLeft((chunkAccess) -> {
                    Chunk chunk = (Chunk) chunkAccess;
                    ((ChunkBridge) chunk).bridge$loadCallback();
                });
            }, ((ChunkManagerBridge) chunkManager).bridge$getCallbackExecutor());

            ((ChunkManagerBridge) chunkManager).bridge$getCallbackExecutor().run();
        }
    }
}
