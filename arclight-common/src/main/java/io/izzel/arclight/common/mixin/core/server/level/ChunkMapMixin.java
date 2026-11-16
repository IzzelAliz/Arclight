package io.izzel.arclight.common.mixin.core.server.level;

import com.mojang.datafixers.DataFixer;
import io.izzel.arclight.common.bridge.core.world.level.WorldBridge;
import io.izzel.arclight.common.bridge.core.server.level.ChunkMapBridge;
import io.izzel.arclight.common.mod.util.ArclightCallbackExecutor;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.thread.BlockableEventLoop;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.chunk.status.WorldGenContext;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.entity.ChunkStatusUpdateListener;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.bukkit.craftbukkit.generator.CustomChunkGenerator;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.util.concurrent.Executor;
import java.util.function.BooleanSupplier;

@Mixin(ChunkMap.class)
public abstract class ChunkMapMixin implements ChunkMapBridge {

    // @formatter:off
    @Shadow @Final public it.unimi.dsi.fastutil.ints.Int2ObjectMap<ChunkMap.TrackedEntity> entityMap;
    @Shadow @Nullable protected abstract ChunkHolder getUpdatingChunkIfPresent(long chunkPosIn);
    @Shadow @Final private Long2ObjectLinkedOpenHashMap<ChunkHolder> visibleChunkMap;
    @Shadow protected abstract void tick();
    @Shadow @Final public ServerLevel level;
    @Shadow @Final @Mutable private RandomState randomState;
    @Shadow @Final @Mutable private ChunkGeneratorStructureState chunkGeneratorState;
    @Shadow @Final @Mutable private WorldGenContext worldGenContext;
    @Invoker("tick") public abstract void bridge$tick(BooleanSupplier hasMoreTime);
    @Invoker("setServerViewDistance") public abstract void bridge$setViewDistance(int i);
    @Invoker("readChunk") public abstract java.util.concurrent.CompletableFuture<java.util.Optional<net.minecraft.nbt.CompoundTag>> bridge$readChunk(net.minecraft.world.level.ChunkPos pos);
    // @formatter:on

    @Inject(method = "<init>", at = @At("RETURN"))
    private void arclight$updateRandom(ServerLevel p_214836_, LevelStorageSource.LevelStorageAccess p_214837_, DataFixer p_214838_, StructureTemplateManager p_214839_, Executor p_214840_, BlockableEventLoop p_214841_, LightChunkGetter p_214842_, ChunkGenerator chunkGenerator, ChunkStatusUpdateListener p_214845_, net.minecraft.world.level.TicketStorage ticketStorage, int p_214847_, boolean p_214848_, CallbackInfo ci) {
        this.bridge$setChunkGenerator(chunkGenerator);
    }

    @Redirect(method = "upgradeChunkTag", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;dimension()Lnet/minecraft/resources/ResourceKey;"))
    private ResourceKey<LevelStem> arclight$useTypeKey(ServerLevel serverWorld) {
        return ((WorldBridge) serverWorld).bridge$getTypeKey();
    }

    @com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod(method = "upgradeChunkTag(Lnet/minecraft/nbt/CompoundTag;)Lnet/minecraft/nbt/CompoundTag;")
    private net.minecraft.nbt.CompoundTag arclight$preserveBelowZeroPolicy(net.minecraft.nbt.CompoundTag tag,
            com.llamalad7.mixinextras.injector.wrapoperation.Operation<net.minecraft.nbt.CompoundTag> original) {
        int version = net.minecraft.nbt.NbtUtils.getDataVersion(tag, -1);
        boolean stopBelowZero = version < net.minecraft.SharedConstants.getCurrentVersion().dataVersion().version()
            && version <= 2730
            && !((WorldBridge) this.level).bridge$spigotConfig().belowZeroGenerationInExistingChunks
            && "full".equals(tag.getCompoundOrEmpty("Level").getStringOr("Status", ""));
        // Keep the native DFU/context/version/error chain intact and call it exactly once.
        net.minecraft.nbt.CompoundTag upgraded = original.call(tag);
        if (stopBelowZero) {
            upgraded.putString("Status", net.minecraft.core.registries.BuiltInRegistries.CHUNK_STATUS
                .getKey(net.minecraft.world.level.chunk.status.ChunkStatus.SPAWN).toString());
        }
        return upgraded;
    }

    @Override
    public void bridge$resendEntityPairingData(net.minecraft.world.entity.Entity entity) {
        if (!entity.isAlive()) return;
        var tracker = this.entityMap.get(entity.getId());
        if (tracker == null) return;
        for (var connection : tracker.seenBy) {
            var player = connection.getPlayer();
            if (!player.getBukkitEntity().canSee(entity.getBukkitEntity())) continue;
            var packets = new java.util.ArrayList<net.minecraft.network.protocol.Packet<? super net.minecraft.network.protocol.game.ClientGamePacketListener>>();
            tracker.serverEntity.sendPairingData(player, packets::add);
            player.connection.send(new net.minecraft.network.protocol.game.ClientboundBundlePacket(packets));
        }
    }

    @Override
    public void bridge$sendEntitySpawnPacket(net.minecraft.world.entity.Entity entity, net.minecraft.server.level.ServerPlayer player) {
        if (!entity.isAlive()) return;
        var tracker = this.entityMap.get(entity.getId());
        if (tracker != null) {
            player.connection.send(entity.getAddEntityPacket(tracker.serverEntity));
        }
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
        return this.visibleChunkMap.values();
    }

    @Override
    public void bridge$tickEntityTracker() {
        this.tick();
    }

    @Override
    public io.papermc.paper.world.chunk.ChunkGenerationState bridge$getChunkGenerationState(int x, int z) {
        var server = this.level.getServer();
        var loaded = server.isSameThread() ? this.arclight$loadedGenerationState(x, z)
            : server.submit(() -> this.arclight$loadedGenerationState(x, z)).join();
        if (loaded != null) {
            return loaded;
        }
        final net.minecraft.nbt.CompoundTag data;
        try {
            data = ((ChunkMap) (Object) this).read(new net.minecraft.world.level.ChunkPos(x, z)).join().orElse(null);
        } catch (java.util.concurrent.CompletionException ex) {
            throw new RuntimeException("Failed to read chunk data at (" + x + ", " + z + ")", ex.getCause());
        }
        return arclight$classifyGenerationState(data, x, z);
    }

    @Override
    public java.util.concurrent.CompletableFuture<io.papermc.paper.world.chunk.ChunkGenerationState> bridge$getChunkGenerationStateAsync(int x, int z) {
        return this.level.getServer().submit(() -> {
            var loaded = this.arclight$loadedGenerationState(x, z);
            if (loaded != null) {
                return java.util.concurrent.CompletableFuture.completedFuture(loaded);
            }
            return ((ChunkMap) (Object) this).read(new net.minecraft.world.level.ChunkPos(x, z))
                .handleAsync((data, failure) -> {
                    if (failure != null) {
                        throw new java.util.concurrent.CompletionException(failure);
                    }
                    return arclight$classifyGenerationState(data.orElse(null), x, z);
                }, net.minecraft.util.Util.backgroundExecutor());
        }).thenCompose(future -> future);
    }

    @org.spongepowered.asm.mixin.Unique
    private io.papermc.paper.world.chunk.ChunkGenerationState arclight$loadedGenerationState(int x, int z) {
        var holder = this.getUpdatingChunkIfPresent(new net.minecraft.world.level.ChunkPos(x, z).pack());
        if (holder == null) {
            return null;
        }
        var chunk = holder.getChunkIfPresentUnchecked(net.minecraft.world.level.chunk.status.ChunkStatus.EMPTY);
        if (chunk == null) {
            return null;
        }
        return chunk.getPersistedStatus().isOrAfter(net.minecraft.world.level.chunk.status.ChunkStatus.FULL)
            ? io.papermc.paper.world.chunk.ChunkGenerationState.GENERATED
            : io.papermc.paper.world.chunk.ChunkGenerationState.PARTIAL;
    }

    @org.spongepowered.asm.mixin.Unique
    private static io.papermc.paper.world.chunk.ChunkGenerationState arclight$classifyGenerationState(net.minecraft.nbt.CompoundTag data, int x, int z) {
        if (data == null) {
            return io.papermc.paper.world.chunk.ChunkGenerationState.NOT_GENERATED;
        }
        int version = net.minecraft.nbt.NbtUtils.getDataVersion(data, -1);
        int current = net.minecraft.SharedConstants.getCurrentVersion().dataVersion().version();
        if (version > current) {
            throw new IllegalStateException("Chunk (" + x + ", " + z + ") was saved by a newer version (data version " + version + ", current " + current + ")");
        }
        if (version < current || data.getIntOr("xPos", 0) != x || data.getIntOr("zPos", 0) != z) {
            return io.papermc.paper.world.chunk.ChunkGenerationState.OUTDATED;
        }
        var status = data.read("Status", net.minecraft.world.level.chunk.status.ChunkStatus.CODEC)
            .orElse(net.minecraft.world.level.chunk.status.ChunkStatus.EMPTY);
        var retrogen = data.read("below_zero_retrogen", net.minecraft.world.level.levelgen.BelowZeroRetrogen.CODEC).orElse(null);
        if (retrogen == null) {
            return status.isOrAfter(net.minecraft.world.level.chunk.status.ChunkStatus.FULL)
                ? io.papermc.paper.world.chunk.ChunkGenerationState.GENERATED
                : io.papermc.paper.world.chunk.ChunkGenerationState.PARTIAL;
        }
        return status.isOrAfter(net.minecraft.world.level.chunk.status.ChunkStatus.FULL)
            || retrogen.targetStatus().isOrAfter(net.minecraft.world.level.chunk.status.ChunkStatus.SPAWN)
            ? io.papermc.paper.world.chunk.ChunkGenerationState.OUTDATED
            : io.papermc.paper.world.chunk.ChunkGenerationState.PARTIAL;
    }

    @Override
    public void bridge$setChunkGenerator(ChunkGenerator generator) {
        var rg = generator;
        if (rg instanceof CustomChunkGenerator custom) {
            rg = custom.getDelegate();
        }
        if (rg instanceof NoiseBasedChunkGenerator noise) {
            this.randomState = RandomState.create(this.level.registryAccess().lookupOrThrow(Registries.NOISE), this.level.getSeed(), noise.generatorSettings().value());
        } else {
            this.randomState = RandomState.create(this.level.registryAccess().lookupOrThrow(Registries.NOISE), this.level.getSeed(), false, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 63, net.minecraft.world.level.levelgen.NoiseRouterData.none());
        }
        this.chunkGeneratorState = generator.createState(level.registryAccess().lookupOrThrow(Registries.STRUCTURE_SET), this.randomState, level.getSeed());
        var old = this.worldGenContext;
        this.worldGenContext = new WorldGenContext(old.level(), generator, old.structureManager(), old.lightEngine(), old.mainThreadExecutor(), old.unsavedListener());
    }
}
