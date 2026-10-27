package io.izzel.arclight.common.mixin.core.server.level;

import io.izzel.arclight.common.bridge.core.server.level.ChunkMapBridge;
import io.izzel.arclight.common.bridge.core.world.server.ChunkHolderBridge;
import io.izzel.arclight.common.bridge.core.world.level.chunk.LevelChunkBridge;
import io.izzel.arclight.common.mod.server.ArclightServer;
import io.izzel.arclight.common.mod.mixins.annotation.LoadIfMod;
import net.minecraft.server.level.*;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Mixin(ChunkHolder.class)
@LoadIfMod(modid = "moonrise", condition = LoadIfMod.ModCondition.ABSENT)
public abstract class ChunkHolderLoadCallbackMixin {
    @Shadow public int oldTicketLevel;
    @Shadow private int ticketLevel;
    @Shadow public abstract CompletableFuture<ChunkResult<LevelChunk>> getFullChunkFuture();

    // Note that this logic is slightly different from the one above
    @Inject(method = "updateFutures", at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/server/level/ChunkHolder$LevelChangeListener;onLevelChange(Lnet/minecraft/world/level/ChunkPos;Ljava/util/function/IntSupplier;ILjava/util/function/IntConsumer;)V"))
    private void arclight$onChunkLoad(ChunkMap chunkManager, Executor executor, CallbackInfo ci) {
        ((ChunkHolderBridge) this).bridge$resetPendingUnload();
        FullChunkStatus fullChunkStatus = ChunkLevel.fullStatus(this.oldTicketLevel);
        FullChunkStatus fullChunkStatus2 = ChunkLevel.fullStatus(this.ticketLevel);
        this.oldTicketLevel = this.ticketLevel;
        if (!fullChunkStatus.isOrAfter(FullChunkStatus.FULL) && fullChunkStatus2.isOrAfter(FullChunkStatus.FULL)) {
            this.getFullChunkFuture().thenAccept((either) -> {
                LevelChunk chunk = either.orElse(null);
                if (chunk != null) {
                    ((ChunkMapBridge) chunkManager).bridge$getCallbackExecutor().execute(
                            ((LevelChunkBridge) chunk)::bridge$loadCallback
                    );
                }
            }).exceptionally((throwable) -> {
                // ensure exceptions are printed, by default this is not the case
                ArclightServer.LOGGER.fatal("Failed to schedule load callback for chunk " + ((ChunkHolder) (Object) this).getPos(), throwable);
                return null;
            });

            ((ChunkMapBridge) chunkManager).bridge$getCallbackExecutor().run();
        }
    }
}
