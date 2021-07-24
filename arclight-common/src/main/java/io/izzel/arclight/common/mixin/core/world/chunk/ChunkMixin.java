package io.izzel.arclight.common.mixin.core.world.chunk;

import io.izzel.arclight.common.bridge.world.WorldBridge;
import io.izzel.arclight.common.bridge.world.chunk.ChunkBridge;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.CraftChunk;
import org.bukkit.craftbukkit.v.persistence.CraftPersistentDataContainer;
import org.bukkit.craftbukkit.v.persistence.CraftPersistentDataTypeRegistry;
import org.bukkit.event.world.ChunkLoadEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ClassInstanceMultiMap;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.TickList;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkBiomeContainer;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.material.Fluid;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

@Mixin(LevelChunk.class)
public abstract class ChunkMixin implements ChunkBridge {

    // @formatter:off
    @Shadow @Nullable public abstract BlockState setBlockState(BlockPos pos, BlockState state, boolean isMoving);
    @Shadow @Final public Level level;
    @Shadow @Final private ChunkPos chunkPos;
    @Shadow private volatile boolean unsaved;
    @Shadow private boolean lastSaveHadEntities;
    @Shadow private long lastSaveTime;
    @Shadow @Final public ClassInstanceMultiMap<Entity>[] entitySections;
    // @formatter:on

    public org.bukkit.Chunk bukkitChunk;
    public boolean mustNotSave;
    public boolean needsDecoration;
    private transient boolean arclight$doPlace;
    public ServerLevel $$world;

    private static final CraftPersistentDataTypeRegistry DATA_TYPE_REGISTRY = new CraftPersistentDataTypeRegistry();
    public final CraftPersistentDataContainer persistentDataContainer = new CraftPersistentDataContainer(DATA_TYPE_REGISTRY);

    @Inject(method = "<init>(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/ChunkPos;Lnet/minecraft/world/level/chunk/ChunkBiomeContainer;Lnet/minecraft/world/level/chunk/UpgradeData;Lnet/minecraft/world/level/TickList;Lnet/minecraft/world/level/TickList;J[Lnet/minecraft/world/level/chunk/LevelChunkSection;Ljava/util/function/Consumer;)V", at = @At("RETURN"))
    private void arclight$init(Level worldIn, ChunkPos chunkPosIn, ChunkBiomeContainer biomeContainerIn, UpgradeData upgradeDataIn, TickList<Block> tickBlocksIn, TickList<Fluid> tickFluidsIn, long inhabitedTimeIn, LevelChunkSection[] sectionsIn, Consumer<LevelChunk> postLoadConsumerIn, CallbackInfo ci) {
        this.$$world = ((ServerLevel) worldIn);
        bridge$setBukkitChunk(new CraftChunk((LevelChunk) (Object) this));
    }

    @SuppressWarnings("unchecked")
    public List<Entity>[] getEntitySlices() {
        return Arrays.stream(this.entitySections).map(ClassInstanceMultiMap::getAllInstances).toArray(List[]::new);
    }

    public org.bukkit.Chunk getBukkitChunk() {
        return bukkitChunk;
    }

    @Override
    public org.bukkit.Chunk bridge$getBukkitChunk() {
        return bukkitChunk;
    }

    @Override
    public CraftPersistentDataContainer bridge$getPersistentContainer() {
        return this.persistentDataContainer;
    }

    @Override
    public void bridge$setBukkitChunk(org.bukkit.Chunk chunk) {
        this.bukkitChunk = chunk;
    }

    public BlockState setType(BlockPos pos, BlockState state, boolean isMoving, boolean doPlace) {
        return this.bridge$setType(pos, state, isMoving, doPlace);
    }

    @Override
    public BlockState bridge$setType(BlockPos pos, BlockState state, boolean isMoving, boolean doPlace) {
        this.arclight$doPlace = doPlace;
        try {
            return this.setBlockState(pos, state, isMoving);
        } finally {
            this.arclight$doPlace = true;
        }
    }

    @Override
    public boolean bridge$isMustNotSave() {
        return this.mustNotSave;
    }

    @Override
    public void bridge$setMustNotSave(boolean mustNotSave) {
        this.mustNotSave = mustNotSave;
    }

    @Override
    public boolean bridge$isNeedsDecoration() {
        return this.needsDecoration;
    }

    @Override
    public void bridge$loadCallback() {
        loadCallback();
    }

    @Override
    public void bridge$unloadCallback() {
        unloadCallback();
    }

    public void loadCallback() {
        org.bukkit.Server server = Bukkit.getServer();
        if (server != null) {
            /*
             * If it's a new world, the first few chunks are generated inside
             * the World constructor. We can't reliably alter that, so we have
             * no way of creating a CraftWorld/CraftServer at that point.
             */
            server.getPluginManager().callEvent(new ChunkLoadEvent(this.bukkitChunk, this.needsDecoration));

            if (this.needsDecoration) {
                this.needsDecoration = false;
                java.util.Random random = new java.util.Random();
                random.setSeed(((ServerLevel) level).getSeed());
                long xRand = random.nextLong() / 2L * 2L + 1L;
                long zRand = random.nextLong() / 2L * 2L + 1L;
                random.setSeed((long) this.chunkPos.x * xRand + (long) this.chunkPos.z * zRand ^ ((ServerLevel) level).getSeed());

                org.bukkit.World world = ((WorldBridge) this.level).bridge$getWorld();
                if (world != null) {
                    ((WorldBridge) this.level).bridge$setPopulating(true);
                    try {
                        for (org.bukkit.generator.BlockPopulator populator : world.getPopulators()) {
                            populator.populate(world, random, bukkitChunk);
                        }
                    } finally {
                        ((WorldBridge) this.level).bridge$setPopulating(false);
                    }
                }
                server.getPluginManager().callEvent(new org.bukkit.event.world.ChunkPopulateEvent(bukkitChunk));
            }
        }
    }

    public void unloadCallback() {
        org.bukkit.Server server = Bukkit.getServer();
        org.bukkit.event.world.ChunkUnloadEvent unloadEvent = new org.bukkit.event.world.ChunkUnloadEvent(this.bukkitChunk, this.isUnsaved());
        server.getPluginManager().callEvent(unloadEvent);
        // note: saving can be prevented, but not forced if no saving is actually required
        this.mustNotSave = !unloadEvent.isSaveChunk();
    }

    @Inject(method = "<init>(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/chunk/ProtoChunk;)V",
        at = @At("RETURN"))
    public void arclight$setNeedsDecoration(Level worldIn, ProtoChunk primer, CallbackInfo ci) {
        this.needsDecoration = true;
    }

    @Redirect(method = "setBlockState", at = @At(value = "FIELD", ordinal = 1, target = "Lnet/minecraft/world/level/Level;isClientSide:Z"))
    public boolean arclight$redirectIsRemote(Level world) {
        return world.isClientSide && this.arclight$doPlace;
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public boolean isUnsaved() {
        return !this.mustNotSave && (this.unsaved || this.lastSaveHadEntities && this.level.getGameTime() != this.lastSaveTime);
    }
}
