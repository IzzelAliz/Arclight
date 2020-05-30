package io.izzel.arclight.common.mixin.core.world.server;

import com.mojang.datafixers.util.Either;
import io.izzel.arclight.common.bridge.world.server.ChunkHolderBridge;
import io.izzel.arclight.common.bridge.world.server.ChunkManagerBridge;
import io.izzel.arclight.common.bridge.world.server.ServerChunkProviderBridge;
import io.izzel.arclight.common.bridge.world.server.TicketManagerBridge;
import net.minecraft.profiler.IProfiler;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.server.ChunkHolder;
import net.minecraft.world.server.ChunkManager;
import net.minecraft.world.server.ServerChunkProvider;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.server.ServerWorldLightManager;
import net.minecraft.world.server.TicketManager;
import net.minecraft.world.server.TicketType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

@Mixin(ServerChunkProvider.class)
public abstract class ServerChunkProviderMixin implements ServerChunkProviderBridge {

    // @formatter:off
    @Shadow public abstract void save(boolean flush);
    @Shadow @Final private ServerWorldLightManager lightManager;
    @Shadow @Final public ChunkManager chunkManager;
    @Shadow @Final public ServerWorld world;
    @Shadow @Final private TicketManager ticketManager;
    @Shadow protected abstract void invalidateCaches();
    @Shadow @Nullable protected abstract ChunkHolder func_217213_a(long chunkPosIn);
    @Shadow protected abstract boolean func_217235_l();
    @Invoker("func_217235_l") public abstract boolean bridge$tickDistanceManager();
    @Accessor("lightManager") public abstract ServerWorldLightManager bridge$getLightManager();
    // @formatter:on

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    private CompletableFuture<Either<IChunk, ChunkHolder.IChunkLoadingError>> func_217233_c(int chunkX, int chunkZ, ChunkStatus requiredStatus, boolean load) {
        ChunkPos chunkpos = new ChunkPos(chunkX, chunkZ);
        long i = chunkpos.asLong();
        int j = 33 + ChunkStatus.getDistance(requiredStatus);
        ChunkHolder chunkholder = this.func_217213_a(i);
        boolean unloading = false;
        if (chunkholder != null) {
            ChunkHolder.LocationType chunkStatus = ChunkHolder.func_219286_c(((ChunkHolderBridge) chunkholder).bridge$getOldTicketLevel());
            ChunkHolder.LocationType currentStatus = ChunkHolder.func_219286_c(chunkholder.func_219299_i());
            unloading = chunkStatus.func_219065_a(ChunkHolder.LocationType.BORDER) && !currentStatus.func_219065_a(ChunkHolder.LocationType.BORDER);
        }
        if (load && !unloading) {
            this.ticketManager.func_219356_a(TicketType.UNKNOWN, chunkpos, j, chunkpos);
            if (this.func_217224_a(chunkholder, j)) {
                IProfiler iprofiler = this.world.getProfiler();
                iprofiler.startSection("chunkLoad");
                this.func_217235_l();
                chunkholder = this.func_217213_a(i);
                iprofiler.endSection();
                if (this.func_217224_a(chunkholder, j)) {
                    throw new IllegalStateException("No chunk holder after ticket has been added");
                }
            }
        }

        return this.func_217224_a(chunkholder, j) ? ChunkHolder.MISSING_CHUNK_FUTURE : chunkholder.func_219276_a(requiredStatus, this.chunkManager);
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    private boolean func_217224_a(@Nullable ChunkHolder chunkHolderIn, int p_217224_2_) {
        return chunkHolderIn == null || ((ChunkHolderBridge) chunkHolderIn).bridge$getOldTicketLevel() > p_217224_2_;
    }

    public void close(boolean save) throws IOException {
        if (save) {
            this.save(true);
        }
        this.lightManager.close();
        this.chunkManager.close();
    }

    public void purgeUnload() {
        this.world.getProfiler().startSection("purge");
        ((TicketManagerBridge) this.ticketManager).bridge$tick();
        this.bridge$tickDistanceManager();
        this.world.getProfiler().endStartSection("unload");
        ((ChunkManagerBridge) this.chunkManager).bridge$tick(() -> true);
        this.world.getProfiler().endSection();
        this.invalidateCaches();
    }

    @Override
    public void bridge$close(boolean save) throws IOException {
        this.close(save);
    }

    @Override
    public void bridge$purgeUnload() {
        this.purgeUnload();
    }

    @Redirect(method = "func_217224_a", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/server/ChunkHolder;func_219299_i()I"))
    public int arclight$useOldTicketLevel(ChunkHolder chunkHolder) {
        return ((ChunkHolderBridge) chunkHolder).bridge$getOldTicketLevel();
    }
}
