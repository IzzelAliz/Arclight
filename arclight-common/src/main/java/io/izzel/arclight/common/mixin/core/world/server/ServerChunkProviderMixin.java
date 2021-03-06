package io.izzel.arclight.common.mixin.core.world.server;

import io.izzel.arclight.common.bridge.world.WorldBridge;
import io.izzel.arclight.common.bridge.world.server.ChunkHolderBridge;
import io.izzel.arclight.common.bridge.world.server.ChunkManagerBridge;
import io.izzel.arclight.common.bridge.world.server.ServerChunkProviderBridge;
import io.izzel.arclight.common.bridge.world.server.TicketManagerBridge;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.GameRules;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.server.ChunkHolder;
import net.minecraft.world.server.ChunkManager;
import net.minecraft.world.server.ServerChunkProvider;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.server.ServerWorldLightManager;
import net.minecraft.world.server.TicketManager;
import net.minecraft.world.storage.IWorldInfo;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.function.Consumer;

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
    @Shadow protected abstract boolean func_217224_a(@Nullable ChunkHolder chunkHolderIn, int p_217224_2_);
    @Shadow public boolean spawnHostiles;
    @Shadow public boolean spawnPassives;
    @Shadow protected abstract void func_241098_a_(long p_241098_1_, Consumer<Chunk> p_241098_3_);
    @Shadow @Final @Mutable public ChunkGenerator generator;
    @Invoker("func_217235_l") public abstract boolean bridge$tickDistanceManager();
    @Accessor("lightManager") public abstract ServerWorldLightManager bridge$getLightManager();
    // @formatter:on

    public boolean isChunkLoaded(final int chunkX, final int chunkZ) {
        final ChunkHolder chunk = ((ChunkManagerBridge) this.chunkManager).bridge$chunkHolderAt(ChunkPos.asLong(chunkX, chunkZ));
        return chunk != null && ((ChunkHolderBridge) chunk).bridge$getFullChunk() != null;
    }

    @Override
    public boolean bridge$isChunkLoaded(int x, int z) {
        return isChunkLoaded(x, z);
    }

    @Override
    public void bridge$setChunkGenerator(ChunkGenerator chunkGenerator) {
        this.generator = chunkGenerator;
        ((ChunkManagerBridge) this.chunkManager).bridge$setChunkGenerator(chunkGenerator);
    }

    @Override
    public void bridge$setViewDistance(int viewDistance) {
        ((ChunkManagerBridge) this.chunkManager).bridge$setViewDistance(viewDistance);
    }

    @ModifyVariable(method = "func_217233_c", index = 4, at = @At("HEAD"))
    private boolean arclight$skipIfUnloading(boolean flag, int chunkX, int chunkZ) {
        if (flag) {
            ChunkHolder chunkholder = this.func_217213_a(ChunkPos.asLong(chunkX, chunkZ));
            if (chunkholder != null) {
                ChunkHolder.LocationType chunkStatus = ChunkHolder.getLocationTypeFromLevel(((ChunkHolderBridge) chunkholder).bridge$getOldTicketLevel());
                ChunkHolder.LocationType currentStatus = ChunkHolder.getLocationTypeFromLevel(chunkholder.getChunkLevel());
                return !chunkStatus.isAtLeast(ChunkHolder.LocationType.BORDER) || currentStatus.isAtLeast(ChunkHolder.LocationType.BORDER);
            } else {
                return true;
            }
        } else {
            return false;
        }
    }

    @Redirect(method = "tickChunks", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/GameRules;getBoolean(Lnet/minecraft/world/GameRules$RuleKey;)Z"))
    private boolean arclight$noPlayer(GameRules gameRules, GameRules.RuleKey<GameRules.BooleanValue> key) {
        return gameRules.getBoolean(key) && !this.world.getPlayers().isEmpty();
    }

    @Redirect(method = "tickChunks", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/storage/IWorldInfo;getGameTime()J"))
    private long arclight$ticksPer(IWorldInfo worldInfo) {
        long gameTime = worldInfo.getGameTime();
        long ticksPer = ((WorldBridge) this.world).bridge$ticksPerAnimalSpawns();
        return (ticksPer != 0L && gameTime % ticksPer == 0) ? 0 : 1;
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

    @Redirect(method = "func_217224_a", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/server/ChunkHolder;getChunkLevel()I"))
    public int arclight$useOldTicketLevel(ChunkHolder chunkHolder) {
        return ((ChunkHolderBridge) chunkHolder).bridge$getOldTicketLevel();
    }
}
