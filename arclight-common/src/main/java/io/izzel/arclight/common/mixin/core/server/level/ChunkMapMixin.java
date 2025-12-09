package io.izzel.arclight.common.mixin.core.server.level;

import com.google.common.collect.Lists;
import com.mojang.datafixers.DataFixer;
import io.izzel.arclight.common.bridge.core.world.WorldBridge;
import io.izzel.arclight.common.bridge.core.world.server.ChunkMapBridge;
import io.izzel.arclight.common.bridge.core.world.server.ChunkMap_TrackedEntityBridge;
import io.izzel.arclight.common.mod.util.ArclightCallbackExecutor;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.*;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.util.thread.BlockableEventLoop;
import net.minecraft.world.entity.Entity;
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
import org.bukkit.craftbukkit.v.generator.CustomChunkGenerator;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

@Mixin(ChunkMap.class)
public abstract class ChunkMapMixin implements ChunkMapBridge {
    // @formatter:off
    @Shadow @Nullable protected abstract ChunkHolder getUpdatingChunkIfPresent(long chunkPosIn);
    @Shadow protected abstract Iterable<ChunkHolder> getChunks();
    @Shadow @Final public ServerLevel level;
    @Shadow @Final @Mutable private RandomState randomState;
    @Shadow @Final @Mutable private ChunkGeneratorStructureState chunkGeneratorState;
    @Shadow @Final @Mutable private WorldGenContext worldGenContext;
    @Invoker("tick") public abstract void bridge$tick(BooleanSupplier hasMoreTime);
    @Invoker("setServerViewDistance") public abstract void bridge$setViewDistance(int i);
    // @formatter:on

    @Shadow
    @Final
    private PlayerMap playerMap;

    @Shadow
    protected abstract void updateChunkTracking(ServerPlayer player);

    @Shadow
    @Final
    public Int2ObjectMap<ChunkMap.TrackedEntity> entityMap;

    @Shadow
    @Final
    public ChunkMap.DistanceManager distanceManager;

    @Unique
    public final ArclightCallbackExecutor arclight$callbackExecutor = new ArclightCallbackExecutor();

    @Inject(method = "<init>", at = @At("RETURN"))
    private void arclight$updateRandom(
            ServerLevel p_214836_,
            LevelStorageSource.LevelStorageAccess p_214837_,
            DataFixer p_214838_,
            StructureTemplateManager p_214839_,
            Executor p_214840_,
            BlockableEventLoop p_214841_,
            LightChunkGetter p_214842_,
            ChunkGenerator chunkGenerator,
            ChunkProgressListener p_214844_,
            ChunkStatusUpdateListener p_214845_,
            Supplier p_214846_,
            int p_214847_,
            boolean p_214848_,
            CallbackInfo ci
    ) {
        this.bridge$setChunkGenerator(chunkGenerator);
    }

    @Redirect(method = "upgradeChunkTag", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;dimension()Lnet/minecraft/resources/ResourceKey;"))
    private ResourceKey<LevelStem> arclight$useTypeKey(ServerLevel serverWorld) {
        return ((WorldBridge) serverWorld).bridge$getTypeKey();
    }

    @Override
    public ArclightCallbackExecutor bridge$getCallbackExecutor() {
        return arclight$callbackExecutor;
    }

    @Override
    public ChunkHolder bridge$chunkHolderAt(long chunkPos) {
        return getUpdatingChunkIfPresent(chunkPos);
    }

    @Override
    public Iterable<ChunkHolder> bridge$getLoadedChunksIterable() {
        return getChunks();
    }

    @Override
    public void bridge$tickEntityTracker() {
        arclight$safeTick();
    }

    @Override
    public void bridge$setChunkGenerator(ChunkGenerator generator) {
        var rg = generator;
        if (rg instanceof CustomChunkGenerator custom) {
            rg = custom.getDelegate();
        }
        if (rg instanceof NoiseBasedChunkGenerator noise) {
            this.randomState = RandomState.create(noise.generatorSettings().value(), this.level.registryAccess().lookupOrThrow(Registries.NOISE), this.level.getSeed());
        } else {
            this.randomState = RandomState.create(NoiseGeneratorSettings.dummy(), this.level.registryAccess().lookupOrThrow(Registries.NOISE), this.level.getSeed());
        }
        this.chunkGeneratorState = generator.createState(level.registryAccess().lookupOrThrow(Registries.STRUCTURE_SET), this.randomState, level.getSeed());
        var old = this.worldGenContext;
        this.worldGenContext = new WorldGenContext(old.level(), generator, old.structureManager(), old.lightEngine(), old.mainThreadMailBox());
    }

    @Inject(method = "tick()V", cancellable = true, at = @At("HEAD"))
    protected void tick(CallbackInfo ci) {
        arclight$safeTick();
        ci.cancel();
    }

    @Unique
    private void arclight$safeTick() {
        for (ServerPlayer serverPlayer : playerMap.getAllPlayers()) {
            updateChunkTracking(serverPlayer);
        }

        List<ServerPlayer> list = Lists.newArrayList();
        List<ServerPlayer> players = level.players();
        List<ChunkMap.TrackedEntity> entities = List.copyOf(entityMap.values());

        for (ChunkMap.TrackedEntity tracked : entities) {
            ChunkMap_TrackedEntityBridge trackedProxy = (ChunkMap_TrackedEntityBridge) tracked;
            SectionPos sectionPos = trackedProxy.bridge$getLastSectionPos();
            SectionPos sectionPos2 = SectionPos.of(trackedProxy.bridge$getEntity());
            boolean sameSection = !Objects.equals(sectionPos, sectionPos2);

            if (sameSection) {
                trackedProxy.bridge$updatePlayers(players);
                Entity entity = trackedProxy.bridge$getEntity();
                if (entity instanceof ServerPlayer) {
                    list.add((ServerPlayer) entity);
                }

                trackedProxy.bridge$setLastSectionPos(sectionPos2);
            }

            if (sameSection || distanceManager.inEntityTickingRange(sectionPos2.chunk().toLong())) {
                trackedProxy.bridge$getServerEntity().sendChanges();
            }
        }

        if (!list.isEmpty()) {
            for (ChunkMap.TrackedEntity entity : entities) {
                entity.updatePlayers(list);
            }
        }
    }
}
