package io.izzel.arclight.common.mixin.core.server.level;

import io.izzel.arclight.common.bridge.core.world.chunk.ChunkBridge;
import io.izzel.arclight.common.bridge.core.world.server.ChunkHolderBridge;
import io.izzel.arclight.common.bridge.core.world.server.ChunkMapBridge;
import io.izzel.arclight.common.mod.server.ArclightServer;
import it.unimi.dsi.fastutil.shorts.ShortSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
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
public abstract class ChunkHolderMixin extends GenerationChunkHolder implements ChunkHolderBridge {

    // @formatter:off
    @Shadow public int oldTicketLevel;
    @Shadow @Final private ShortSet[] changedBlocksPerSection;
    @Shadow @Final private LevelHeightAccessor levelHeightAccessor;
    @Shadow private int ticketLevel;
    @Shadow public abstract CompletableFuture<ChunkResult<LevelChunk>> getFullChunkFuture();@Override @Accessor("oldTicketLevel") public abstract int bridge$getOldTicketLevel();
    // @formatter:on

    public ChunkHolderMixin(ChunkPos chunkPos) {
        super(chunkPos);
    }

    public LevelChunk getFullChunkNow() {
        if (!ChunkLevel.fullStatus(this.oldTicketLevel).isOrAfter(FullChunkStatus.FULL)) return null;
        return this.getFullChunkNowUnchecked();
    }

    public LevelChunk getFullChunkNowUnchecked() {
        return (LevelChunk) this.getChunkIfPresentUnchecked(ChunkStatus.FULL);
    }

    @Override
    public LevelChunk bridge$getFullChunkNow() {
        return this.getFullChunkNow();
    }

    @Override
    public LevelChunk bridge$getFullChunkUnchecked() {
        return this.getFullChunkNowUnchecked();
    }

    @Override
    public void bridge$callEventIfUnloading(ChunkMap manager) {
        callEventIfUnloading(manager);
    }

    protected void callEventIfUnloading(ChunkMap manager) {
        FullChunkStatus oldFullChunkStatus = ChunkLevel.fullStatus(this.oldTicketLevel);
        FullChunkStatus newFullChunkStatus = ChunkLevel.fullStatus(this.ticketLevel);
        boolean oldIsFull = oldFullChunkStatus.isOrAfter(FullChunkStatus.FULL);
        boolean newIsFull = newFullChunkStatus.isOrAfter(FullChunkStatus.FULL);
        if (oldIsFull && !newIsFull) {
            this.getFullChunkFuture().thenAccept((either) -> {
                LevelChunk chunk = either.orElse(null);
                if (chunk != null) {
                    ((ChunkMapBridge)manager).bridge$getCallbackExecutor().execute(() -> {
                        // Minecraft will apply the chunks tick lists to the world once the chunk got loaded, and then store the tick
                        // lists again inside the chunk once the chunk becomes inaccessible and set the chunk's needsSaving flag.
                        // These actions may however happen deferred, so we manually set the needsSaving flag already here.
                        chunk.setUnsaved(true);
                        ((ChunkBridge)chunk).bridge$unloadCallback();
                    });
                }
            }).exceptionally((throwable) -> {
                // ensure exceptions are printed, by default this is not the case
                MinecraftServer.LOGGER.error("Failed to schedule unload callback for chunk " + pos, throwable);
                return null;
            });

            // Run callback right away if the future was already done
            ((ChunkMapBridge) manager).bridge$getCallbackExecutor().run();
        }
    }

    @Inject(method = "blockChanged", cancellable = true,
            at = @At(value = "FIELD", ordinal = 0, target = "Lnet/minecraft/server/level/ChunkHolder;changedBlocksPerSection:[Lit/unimi/dsi/fastutil/shorts/ShortSet;"))
    private void arclight$outOfBound(BlockPos pos, CallbackInfo ci) {
        int i = this.levelHeightAccessor.getSectionIndex(pos.getY());
        if (i < 0 || i >= this.changedBlocksPerSection.length) {
            ci.cancel();
        }
    }

    // Note that this logic is slightly different from the one above
    @Inject(method = "updateFutures", at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/server/level/ChunkHolder$LevelChangeListener;onLevelChange(Lnet/minecraft/world/level/ChunkPos;Ljava/util/function/IntSupplier;ILjava/util/function/IntConsumer;)V"))
    private void arclight$onChunkLoad(ChunkMap chunkManager, Executor executor, CallbackInfo ci) {
        FullChunkStatus fullChunkStatus = ChunkLevel.fullStatus(this.oldTicketLevel);
        FullChunkStatus fullChunkStatus2 = ChunkLevel.fullStatus(this.ticketLevel);
        this.oldTicketLevel = this.ticketLevel;
        if (!fullChunkStatus.isOrAfter(FullChunkStatus.FULL) && fullChunkStatus2.isOrAfter(FullChunkStatus.FULL)) {
            this.getFullChunkFuture().thenAccept((either) -> {
                LevelChunk chunk = either.orElse(null);
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
