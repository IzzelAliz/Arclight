package io.izzel.arclight.common.mixin.core.world.server;

import com.mojang.datafixers.util.Either;
import io.izzel.arclight.common.bridge.world.chunk.ChunkBridge;
import io.izzel.arclight.common.bridge.world.server.ChunkHolderBridge;
import io.izzel.arclight.common.bridge.world.server.ChunkManagerBridge;
import io.izzel.arclight.common.mod.ArclightMod;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.server.ChunkHolder;
import net.minecraft.world.server.ChunkManager;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
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
    @Shadow public int prevChunkLevel;
    @Shadow public abstract CompletableFuture<Either<IChunk, ChunkHolder.IChunkLoadingError>> func_219301_a(ChunkStatus p_219301_1_);
    @Shadow @Final private ChunkPos pos;
    @Override @Accessor("prevChunkLevel") public abstract int bridge$getOldTicketLevel();
    // @formatter:on

    public Chunk getFullChunk() {
        if (!ChunkHolder.getLocationTypeFromLevel(this.prevChunkLevel).isAtLeast(ChunkHolder.LocationType.BORDER)) {
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

    @Inject(method = "processUpdates", at = @At(value = "JUMP", opcode = Opcodes.IFEQ, ordinal = 0),
        locals = LocalCapture.CAPTURE_FAILHARD)
    public void arclight$onChunkUnload(ChunkManager chunkManager, CallbackInfo ci, ChunkStatus chunkStatus,
                                       ChunkStatus chunkStatus1, boolean flag, boolean flag1,
                                       ChunkHolder.LocationType locationType, ChunkHolder.LocationType locationType1) {
        if (locationType.isAtLeast(ChunkHolder.LocationType.BORDER) && !locationType1.isAtLeast(ChunkHolder.LocationType.BORDER)) {
            this.func_219301_a(ChunkStatus.FULL).thenAccept((either) -> {
                Chunk chunk = (Chunk) either.left().orElse(null);
                if (chunk != null) {
                    ((ChunkManagerBridge) chunkManager).bridge$getCallbackExecutor().execute(() -> {
                        chunk.setModified(true);
                        ((ChunkBridge) chunk).bridge$unloadCallback();
                    });
                }
            }).exceptionally((throwable) -> {
                // ensure exceptions are printed, by default this is not the case
                ArclightMod.LOGGER.fatal("Failed to schedule unload callback for chunk " + this.pos, throwable);
                return null;
            });

            // Run callback right away if the future was already done
            ((ChunkManagerBridge) chunkManager).bridge$getCallbackExecutor().run();
        }
    }

    @Inject(method = "processUpdates", at = @At("RETURN"), locals = LocalCapture.CAPTURE_FAILHARD)
    public void arclight$onChunkLoad(ChunkManager chunkManager, CallbackInfo ci, ChunkStatus chunkStatus,
                                     ChunkStatus chunkStatus1, boolean flag, boolean flag1,
                                     ChunkHolder.LocationType locationType, ChunkHolder.LocationType locationType1) {
        if (!locationType.isAtLeast(ChunkHolder.LocationType.BORDER) && locationType1.isAtLeast(ChunkHolder.LocationType.BORDER)) {
            this.func_219301_a(ChunkStatus.FULL).thenAccept((either) -> {
                Chunk chunk = (Chunk) either.left().orElse(null);
                if (chunk != null) {
                    ((ChunkManagerBridge) chunkManager).bridge$getCallbackExecutor().execute(
                        ((ChunkBridge) chunk)::bridge$loadCallback
                    );
                }
            }).exceptionally((throwable) -> {
                // ensure exceptions are printed, by default this is not the case
                ArclightMod.LOGGER.fatal("Failed to schedule load callback for chunk " + this.pos, throwable);
                return null;
            });

            ((ChunkManagerBridge) chunkManager).bridge$getCallbackExecutor().run();
        }
    }
}
