package io.izzel.arclight.common.mixin.core.world.server;

import com.mojang.datafixers.util.Either;
import io.izzel.arclight.common.bridge.world.WorldBridge;
import io.izzel.arclight.common.bridge.world.server.ChunkHolderBridge;
import io.izzel.arclight.common.bridge.world.server.ChunkManagerBridge;
import io.izzel.arclight.common.bridge.world.server.ServerChunkProviderBridge;
import io.izzel.arclight.common.bridge.world.server.TicketManagerBridge;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.entity.EntityClassification;
import net.minecraft.profiler.IProfiler;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.GameRules;
import net.minecraft.world.WorldType;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.server.ChunkHolder;
import net.minecraft.world.server.ChunkManager;
import net.minecraft.world.server.ServerChunkProvider;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.server.ServerWorldLightManager;
import net.minecraft.world.server.TicketManager;
import net.minecraft.world.server.TicketType;
import net.minecraft.world.storage.WorldInfo;
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
import java.util.Optional;
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
    @Shadow protected abstract boolean func_217224_a(@Nullable ChunkHolder chunkHolderIn, int p_217224_2_);
    @Shadow private long lastGameTime;
    @Shadow public boolean spawnHostiles;
    @Shadow public boolean spawnPassives;
    @Shadow @Final private static int field_217238_b;
    @Shadow @Final public ChunkGenerator<?> generator;
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
            ChunkHolder.LocationType chunkStatus = ChunkHolder.getLocationTypeFromLevel(((ChunkHolderBridge) chunkholder).bridge$getOldTicketLevel());
            ChunkHolder.LocationType currentStatus = ChunkHolder.getLocationTypeFromLevel(chunkholder.getChunkLevel());
            unloading = chunkStatus.isAtLeast(ChunkHolder.LocationType.BORDER) && !currentStatus.isAtLeast(ChunkHolder.LocationType.BORDER);
        }
        if (load && !unloading) {
            this.ticketManager.registerWithLevel(TicketType.UNKNOWN, chunkpos, j, chunkpos);
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
    private void tickChunks() {
        long i = this.world.getGameTime();
        long j = i - this.lastGameTime;
        this.lastGameTime = i;
        WorldInfo worldinfo = this.world.getWorldInfo();
        boolean flag = worldinfo.getGenerator() == WorldType.DEBUG_ALL_BLOCK_STATES;
        boolean flag1 = this.world.getGameRules().getBoolean(GameRules.DO_MOB_SPAWNING) && !this.world.getPlayers().isEmpty();
        if (!flag) {
            this.world.getProfiler().startSection("pollingChunks");
            int k = this.world.getGameRules().getInt(GameRules.RANDOM_TICK_SPEED);
            BlockPos blockpos = this.world.getSpawnPoint();

            boolean spawnAnimal = ((WorldBridge) this.world).bridge$ticksPerAnimalSpawns() != 0 && worldinfo.getGameTime() % ((WorldBridge) this.world).bridge$ticksPerAnimalSpawns() == 0;
            boolean spawnMonster = ((WorldBridge) this.world).bridge$ticksPerMonsterSpawns() != 0 && worldinfo.getGameTime() % ((WorldBridge) this.world).bridge$ticksPerMonsterSpawns() == 0;
            boolean spawnWater = ((WorldBridge) this.world).bridge$ticksPerWaterSpawns() != 0 && worldinfo.getGameTime() % ((WorldBridge) this.world).bridge$ticksPerWaterSpawns() == 0;
            boolean spawnAmbient = ((WorldBridge) this.world).bridge$ticksPerAmbientSpawns() != 0 && worldinfo.getGameTime() % ((WorldBridge) this.world).bridge$ticksPerAmbientSpawns() == 0;
            boolean flag2 = spawnAnimal;

            this.world.getProfiler().startSection("naturalSpawnCount");
            int l = this.ticketManager.getSpawningChunksCount();
            EntityClassification[] aentityclassification = EntityClassification.values();
            Object2IntMap<EntityClassification> object2intmap = this.world.countEntities();
            this.world.getProfiler().endSection();
            ((ChunkManagerBridge) this.chunkManager).bridge$getLoadedChunksIterable().forEach((p_223434_10_) -> {
                Optional<Chunk> optional = p_223434_10_.getEntityTickingFuture().getNow(ChunkHolder.UNLOADED_CHUNK).left();
                if (optional.isPresent()) {
                    Chunk chunk = optional.get();
                    this.world.getProfiler().startSection("broadcast");
                    p_223434_10_.sendChanges(chunk);
                    this.world.getProfiler().endSection();
                    ChunkPos chunkpos = p_223434_10_.getPosition();
                    if (!((ChunkManagerBridge) this.chunkManager).bridge$isOutsideSpawningRadius(chunkpos)) {
                        chunk.setInhabitedTime(chunk.getInhabitedTime() + j);
                        if (flag1 && (this.spawnHostiles || this.spawnPassives) && this.world.getWorldBorder().contains(chunk.getPos())) {
                            this.world.getProfiler().startSection("spawner");

                            for (EntityClassification entityclassification : aentityclassification) {

                                boolean spawnThisTick = true;
                                int limit = entityclassification.getMaxNumberOfCreature();
                                switch (entityclassification) {
                                    case MONSTER:
                                        spawnThisTick = spawnMonster;
                                        limit = ((WorldBridge) world).bridge$getWorld().getMonsterSpawnLimit();
                                        break;
                                    case CREATURE:
                                        spawnThisTick = spawnAnimal;
                                        limit = ((WorldBridge) world).bridge$getWorld().getAnimalSpawnLimit();
                                        break;
                                    case WATER_CREATURE:
                                        spawnThisTick = spawnWater;
                                        limit = ((WorldBridge) world).bridge$getWorld().getWaterAnimalSpawnLimit();
                                        break;
                                    case AMBIENT:
                                        spawnThisTick = spawnAmbient;
                                        limit = ((WorldBridge) world).bridge$getWorld().getAmbientSpawnLimit();
                                        break;
                                }

                                if (!spawnThisTick || limit == 0) {
                                    continue;
                                }
                                if (entityclassification != EntityClassification.MISC && (!entityclassification.getPeacefulCreature() || this.spawnPassives) && (entityclassification.getPeacefulCreature() || this.spawnHostiles) && (!entityclassification.getAnimal() || flag2)) {
                                    int i1 = limit * l / field_217238_b;
                                    if (object2intmap.getInt(entityclassification) <= i1) {
                                        this.bridge$worldNaturalSpawn(entityclassification, this.world, chunk, blockpos);
                                    }
                                }
                            }

                            this.world.getProfiler().endSection();
                        }

                        this.world.tickEnvironment(chunk, k);
                    }
                }
            });
            this.world.getProfiler().startSection("customSpawners");
            if (flag1) {
                this.generator.spawnMobs(this.world, this.spawnHostiles, this.spawnPassives);
            }

            this.world.getProfiler().endSection();
            this.world.getProfiler().endSection();
        }

        ((ChunkManagerBridge) this.chunkManager).bridge$tickEntityTracker();
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
