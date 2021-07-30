package io.izzel.arclight.common.mixin.core.server.level;

import com.mojang.datafixers.util.Either;
import io.izzel.arclight.common.bridge.core.world.chunk.ChunkBridge;
import io.izzel.arclight.common.bridge.core.world.server.ChunkHolderBridge;
import io.izzel.arclight.common.bridge.core.world.server.ChunkMapBridge;
import io.izzel.arclight.common.mod.ArclightMod;
import it.unimi.dsi.fastutil.shorts.ShortSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;
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
import java.util.concurrent.Executor;

@Mixin(ChunkHolder.class)
public abstract class ChunkHolderMixin implements ChunkHolderBridge {

    // @formatter:off
    @Shadow public int oldTicketLevel;
    @Shadow public abstract CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> getFutureIfPresentUnchecked(ChunkStatus p_219301_1_);
    @Shadow @Final ChunkPos pos;
    @Shadow @Final private ShortSet[] changedBlocksPerSection;
    @Override @Accessor("oldTicketLevel") public abstract int bridge$getOldTicketLevel();
    // @formatter:on

    public LevelChunk getFullChunk() {
        if (!ChunkHolder.getFullChunkStatus(this.oldTicketLevel).isOrAfter(ChunkHolder.FullChunkStatus.BORDER)) {
            return null; // note: using oldTicketLevel for isLoaded checks
        }
        return this.getFullChunkUnchecked();
    }

    public LevelChunk getFullChunkUnchecked() {
        CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> statusFuture = this.getFutureIfPresentUnchecked(ChunkStatus.FULL);
        Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure> either = statusFuture.getNow(null);
        return (either == null) ? null : (LevelChunk) either.left().orElse(null);
    }

    @Override
    public LevelChunk bridge$getFullChunk() {
        return this.getFullChunk();
    }

    @Override
    public LevelChunk bridge$getFullChunkUnchecked() {
        return this.getFullChunkUnchecked();
    }

    @Inject(method = "blockChanged", cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD,
        at = @At(value = "FIELD", ordinal = 0, target = "Lnet/minecraft/server/level/ChunkHolder;changedBlocksPerSection:[Lit/unimi/dsi/fastutil/shorts/ShortSet;"))
    private void arclight$outOfBound(BlockPos pos, CallbackInfo ci, LevelChunk chunk, int i) {
        if (i < 0 || i >= this.changedBlocksPerSection.length) {
            ci.cancel();
        }
    }

    @Inject(method = "updateFutures", at = @At(value = "JUMP", opcode = Opcodes.IFEQ, ordinal = 0),
        locals = LocalCapture.CAPTURE_FAILHARD)
    public void arclight$onChunkUnload(ChunkMap chunkManager, Executor executor, CallbackInfo ci, ChunkStatus chunkStatus,
                                       ChunkStatus chunkStatus1, boolean flag, boolean flag1,
                                       ChunkHolder.FullChunkStatus locationType, ChunkHolder.FullChunkStatus locationType1) {
        if (locationType.isOrAfter(ChunkHolder.FullChunkStatus.BORDER) && !locationType1.isOrAfter(ChunkHolder.FullChunkStatus.BORDER)) {
            this.getFutureIfPresentUnchecked(ChunkStatus.FULL).thenAccept((either) -> {
                LevelChunk chunk = (LevelChunk) either.left().orElse(null);
                if (chunk != null) {
                    ((ChunkMapBridge) chunkManager).bridge$getCallbackExecutor().execute(() -> {
                        chunk.setUnsaved(true);
                        ((ChunkBridge) chunk).bridge$unloadCallback();
                    });
                }
            }).exceptionally((throwable) -> {
                // ensure exceptions are printed, by default this is not the case
                ArclightMod.LOGGER.fatal("Failed to schedule unload callback for chunk " + this.pos, throwable);
                return null;
            });

            // Run callback right away if the future was already done
            ((ChunkMapBridge) chunkManager).bridge$getCallbackExecutor().run();
        }
    }

    @Inject(method = "updateFutures", at = @At("RETURN"), locals = LocalCapture.CAPTURE_FAILHARD)
    public void arclight$onChunkLoad(ChunkMap chunkManager, Executor executor, CallbackInfo ci, ChunkStatus chunkStatus,
                                     ChunkStatus chunkStatus1, boolean flag, boolean flag1,
                                     ChunkHolder.FullChunkStatus locationType, ChunkHolder.FullChunkStatus locationType1) {
        if (!locationType.isOrAfter(ChunkHolder.FullChunkStatus.BORDER) && locationType1.isOrAfter(ChunkHolder.FullChunkStatus.BORDER)) {
            this.getFutureIfPresentUnchecked(ChunkStatus.FULL).thenAccept((either) -> {
                LevelChunk chunk = (LevelChunk) either.left().orElse(null);
                if (chunk != null) {
                    ((ChunkMapBridge) chunkManager).bridge$getCallbackExecutor().execute(
                        ((ChunkBridge) chunk)::bridge$loadCallback
                    );
                }
            }).exceptionally((throwable) -> {
                // ensure exceptions are printed, by default this is not the case
                ArclightMod.LOGGER.fatal("Failed to schedule load callback for chunk " + this.pos, throwable);
                return null;
            });

            ((ChunkMapBridge) chunkManager).bridge$getCallbackExecutor().run();
        }
    }
}
