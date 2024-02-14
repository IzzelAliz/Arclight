package io.izzel.arclight.common.mixin.core.server.level;

import com.mojang.datafixers.util.Either;
import io.izzel.arclight.common.bridge.core.world.chunk.ChunkBridge;
import io.izzel.arclight.common.bridge.core.world.server.ChunkHolderBridge;
import io.izzel.arclight.common.bridge.core.world.server.ChunkMapBridge;
import io.izzel.arclight.common.mod.server.ArclightServer;
import it.unimi.dsi.fastutil.shorts.ShortSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkLevel;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Mixin(ChunkHolder.class)
public abstract class ChunkHolderMixin implements ChunkHolderBridge {

    // @formatter:off
    @Shadow public int oldTicketLevel;
    @Shadow public abstract CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> getFutureIfPresentUnchecked(ChunkStatus p_219301_1_);
    @Shadow @Final ChunkPos pos;
    @Shadow @Final private ShortSet[] changedBlocksPerSection;
    @Shadow @Final private LevelHeightAccessor levelHeightAccessor;
    @Shadow private int ticketLevel;
    @Override @Accessor("oldTicketLevel") public abstract int bridge$getOldTicketLevel();
    // @formatter:on

    public LevelChunk getFullChunkNow() {
        if (!ChunkLevel.fullStatus(this.oldTicketLevel).isOrAfter(FullChunkStatus.FULL)) {
            return null; // note: using oldTicketLevel for isLoaded checks
        }
        return this.getFullChunkNowUnchecked();
    }

    public LevelChunk getFullChunkNowUnchecked() {
        CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> statusFuture = this.getFutureIfPresentUnchecked(ChunkStatus.FULL);
        Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure> either = statusFuture.getNow(null);
        return (either == null) ? null : (LevelChunk) either.left().orElse(null);
    }

    @Override
    public LevelChunk bridge$getFullChunk() {
        return this.getFullChunkNow();
    }

    @Override
    public LevelChunk bridge$getFullChunkUnchecked() {
        return this.getFullChunkNowUnchecked();
    }

    @Inject(method = "blockChanged", cancellable = true,
        at = @At(value = "FIELD", ordinal = 0, target = "Lnet/minecraft/server/level/ChunkHolder;changedBlocksPerSection:[Lit/unimi/dsi/fastutil/shorts/ShortSet;"))
    private void arclight$outOfBound(BlockPos pos, CallbackInfo ci) {
        int i = this.levelHeightAccessor.getSectionIndex(pos.getY());
        if (i < 0 || i >= this.changedBlocksPerSection.length) {
            ci.cancel();
        }
    }

    @Inject(method = "updateFutures", at = @At(value = "JUMP", opcode = Opcodes.IFEQ, ordinal = 0))
    private void arclight$onChunkUnload(ChunkMap chunkManager, Executor executor, CallbackInfo ci) {
        FullChunkStatus fullChunkStatus = ChunkLevel.fullStatus(this.oldTicketLevel);
        FullChunkStatus fullChunkStatus2 = ChunkLevel.fullStatus(this.ticketLevel);
        if (fullChunkStatus.isOrAfter(FullChunkStatus.FULL) && !fullChunkStatus2.isOrAfter(FullChunkStatus.FULL)) {
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
                ArclightServer.LOGGER.fatal("Failed to schedule unload callback for chunk " + this.pos, throwable);
                return null;
            });

            // Run callback right away if the future was already done
            ((ChunkMapBridge) chunkManager).bridge$getCallbackExecutor().run();
        }
    }

    @Inject(method = "updateFutures", at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/server/level/ChunkHolder$LevelChangeListener;onLevelChange(Lnet/minecraft/world/level/ChunkPos;Ljava/util/function/IntSupplier;ILjava/util/function/IntConsumer;)V"))
    private void arclight$onChunkLoad(ChunkMap chunkManager, Executor executor, CallbackInfo ci) {
        FullChunkStatus fullChunkStatus = ChunkLevel.fullStatus(this.oldTicketLevel);
        FullChunkStatus fullChunkStatus2 = ChunkLevel.fullStatus(this.ticketLevel);
        this.oldTicketLevel = this.ticketLevel;
        if (!fullChunkStatus.isOrAfter(FullChunkStatus.FULL) && fullChunkStatus2.isOrAfter(FullChunkStatus.FULL)) {
            this.getFutureIfPresentUnchecked(ChunkStatus.FULL).thenAccept((either) -> {
                LevelChunk chunk = (LevelChunk) either.left().orElse(null);
                if (chunk != null) {
                    ((ChunkMapBridge) chunkManager).bridge$getCallbackExecutor().execute(
                        ((ChunkBridge) chunk)::bridge$loadCallback
                    );
                }
            }).exceptionally((throwable) -> {
                // ensure exceptions are printed, by default this is not the case
                ArclightServer.LOGGER.fatal("Failed to schedule load callback for chunk " + this.pos, throwable);
                return null;
            });

            ((ChunkMapBridge) chunkManager).bridge$getCallbackExecutor().run();
        }
    }
}
