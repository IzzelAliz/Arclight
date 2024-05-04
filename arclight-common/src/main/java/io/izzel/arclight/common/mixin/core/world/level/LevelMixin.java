package io.izzel.arclight.common.mixin.core.world.level;

import io.izzel.arclight.common.bridge.core.world.WorldBridge;
import io.izzel.arclight.common.bridge.core.world.border.WorldBorderBridge;
import io.izzel.arclight.common.bridge.core.world.level.levelgen.ChunkGeneratorBridge;
import io.izzel.arclight.common.bridge.core.world.server.ServerChunkProviderBridge;
import io.izzel.arclight.common.bridge.core.world.server.ServerWorldBridge;
import io.izzel.arclight.common.mod.ArclightConstants;
import io.izzel.arclight.common.mod.mixins.annotation.CreateConstructor;
import io.izzel.arclight.common.mod.mixins.annotation.ShadowConstructor;
import io.izzel.arclight.common.mod.mixins.annotation.TransformAccess;
import io.izzel.arclight.common.mod.server.ArclightServer;
import io.izzel.arclight.common.mod.server.world.WrappedWorlds;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelWriter;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.level.storage.WritableLevelData;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.CraftServer;
import org.bukkit.craftbukkit.v.CraftWorld;
import org.bukkit.craftbukkit.v.block.CapturedBlockState;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.craftbukkit.v.block.data.CraftBlockData;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.craftbukkit.v.generator.CraftWorldInfo;
import org.bukkit.craftbukkit.v.generator.CustomChunkGenerator;
import org.bukkit.craftbukkit.v.generator.CustomWorldChunkManager;
import org.bukkit.craftbukkit.v.util.CraftSpawnCategory;
import org.bukkit.entity.SpawnCategory;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.generator.ChunkGenerator;
import org.objectweb.asm.Opcodes;
import org.spigotmc.SpigotWorldConfig;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

@Mixin(Level.class)
public abstract class LevelMixin implements WorldBridge, LevelAccessor, LevelWriter {

    // @formatter:off
    @Shadow @Nullable public BlockEntity getBlockEntity(BlockPos pos) { return null; }
    @Shadow public abstract BlockState getBlockState(BlockPos pos);
    @Shadow public abstract WorldBorder getWorldBorder();
    @Shadow @Final private WorldBorder worldBorder;
    @Shadow public abstract long getDayTime();
    @Shadow public abstract MinecraftServer getServer();
    @Shadow public abstract LevelData getLevelData();
    @Shadow public abstract ResourceKey<Level> dimension();
    @Shadow public abstract DimensionType dimensionType();
    @Shadow public abstract void setBlocksDirty(BlockPos arg, BlockState arg2, BlockState arg3);@Shadow@Final public boolean isClientSide;@Shadow public abstract void sendBlockUpdated(BlockPos arg, BlockState arg2, BlockState arg3, int i);@Shadow public abstract void updateNeighbourForOutputSignal(BlockPos arg, Block arg2);@Shadow public abstract void onBlockStateChange(BlockPos arg, BlockState arg2, BlockState arg3);@Accessor("thread") public abstract Thread arclight$getMainThread();
    // @formatter:on

    protected CraftWorld world;
    public boolean pvpMode;
    public boolean keepSpawnInMemory = true;
    public final Object2LongOpenHashMap<SpawnCategory> ticksPerSpawnCategory = new Object2LongOpenHashMap<>();
    public boolean populating;
    public org.bukkit.generator.ChunkGenerator generator;
    protected org.bukkit.World.Environment environment;
    protected org.bukkit.generator.BiomeProvider biomeProvider;
    public org.spigotmc.SpigotWorldConfig spigotConfig;
    @TransformAccess(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC)
    private static BlockPos lastPhysicsProblem; // Spigot
    public boolean preventPoiUpdated = false;
    public Map<BlockPos, CapturedBlockState> capturedBlockStates = new java.util.LinkedHashMap<>();
    public Map<BlockPos, BlockEntity> capturedTileEntities = new HashMap<>();

    @ShadowConstructor
    public void arclight$constructor(WritableLevelData worldInfo, ResourceKey<Level> dimension, RegistryAccess registryAccess, final Holder<DimensionType> dimensionType, Supplier<ProfilerFiller> profiler, boolean isRemote, boolean isDebug, long seed, int maxNeighborUpdate) {
        throw new RuntimeException();
    }

    @CreateConstructor
    public void arclight$constructor(WritableLevelData worldInfo, ResourceKey<Level> dimension, RegistryAccess registryAccess, final Holder<DimensionType> dimensionType, Supplier<ProfilerFiller> profiler, boolean isRemote, boolean isDebug, long seed, int maxNeighborUpdate, org.bukkit.generator.ChunkGenerator gen, org.bukkit.generator.BiomeProvider biomeProvider, org.bukkit.World.Environment env) {
        arclight$constructor(worldInfo, dimension, registryAccess, dimensionType, profiler, isRemote, isDebug, seed, maxNeighborUpdate);
        this.generator = gen;
        this.environment = env;
        this.biomeProvider = biomeProvider;
        bridge$getWorld();
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void arclight$init(WritableLevelData info, ResourceKey<Level> dimension, RegistryAccess registryAccess, Holder<DimensionType> dimType, Supplier<ProfilerFiller> profiler, boolean isRemote, boolean isDebug, long seed, int maxNeighborUpdates, CallbackInfo ci) {
        ((WorldBorderBridge) this.worldBorder).bridge$setWorld((Level) (Object) this);
        for (SpawnCategory spawnCategory : SpawnCategory.values()) {
            if (CraftSpawnCategory.isValidForLimits(spawnCategory)) {
                this.ticksPerSpawnCategory.put(spawnCategory, this.getCraftServer().getTicksPerSpawns(spawnCategory));
            }
        }
    }

    @Override
    public Map<BlockPos, CapturedBlockState> bridge$getCapturedBlockState() {
        return this.capturedBlockStates;
    }

    @Override
    public Map<BlockPos, BlockEntity> bridge$getCapturedBlockEntity() {
        return this.capturedTileEntities;
    }

    @Override
    public void bridge$setLastPhysicsProblem(BlockPos pos) {
        lastPhysicsProblem = pos;
    }

    @Override
    public Object2LongOpenHashMap<SpawnCategory> bridge$ticksPerSpawnCategory() {
        return this.ticksPerSpawnCategory;
    }

    public abstract ResourceKey<LevelStem> getTypeKey();

    @Override
    public ResourceKey<LevelStem> bridge$getTypeKey() {
        return getTypeKey();
    }

    @Override
    public SpigotWorldConfig bridge$spigotConfig() {
        return this.spigotConfig;
    }

    @Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z",
        at = @At("HEAD"), cancellable = true)
    private void arclight$hooks(BlockPos pos, BlockState newState, int flags, CallbackInfoReturnable<Boolean> cir) {
        if (!processCaptures(pos, newState, flags)) {
            cir.setReturnValue(false);
        }
    }

    private boolean processCaptures(BlockPos pos, BlockState newState, int flags) {
        Entity entityChangeBlock = ArclightCaptures.getEntityChangeBlock();
        if (entityChangeBlock != null) {
            return CraftEventFactory.callEntityChangeBlockEvent(entityChangeBlock, pos, newState);
        }
        return true;
    }

    @Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z", require = 0, cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;updateNeighbourShapes(Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;II)V"))
    private void arclight$callBlockPhysics(BlockPos pos, BlockState state, int i, int j, CallbackInfoReturnable<Boolean> cir) {
        try {
            if (this.world != null) {
                BlockPhysicsEvent event = new BlockPhysicsEvent(CraftBlock.at(this, pos), CraftBlockData.fromData(state));
                Bukkit.getPluginManager().callEvent(event);
                if (event.isCancelled()) {
                    cir.setReturnValue(true);
                }
            }
        } catch (StackOverflowError e) {
            lastPhysicsProblem = pos;
        }
    }

    @Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z", require = 0, cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;onBlockStateChange(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;)V"))
    private void arclight$preventPoiUpdate(BlockPos blockPos, BlockState blockState, int i, int j, CallbackInfoReturnable<Boolean> cir) {
        if (this.preventPoiUpdated) {
            cir.setReturnValue(true);
        }
    }

    public void notifyAndUpdatePhysics(BlockPos pos, LevelChunk chunk, BlockState oldBlock, BlockState newBlock, BlockState actualBlock, int i, int j) {
        this.bridge$forge$notifyAndUpdatePhysics(pos, chunk, oldBlock, newBlock, i, j);
    }

    @Override
    public void bridge$forge$notifyAndUpdatePhysics(BlockPos pos, @Nullable LevelChunk levelchunk, BlockState oldBlock, BlockState newBlock, int j, int k) {
        Block block = newBlock.getBlock();
        BlockState blockstate1 = this.getBlockState(pos);
        if (blockstate1 == newBlock) {
            if (oldBlock != blockstate1) {
                this.setBlocksDirty(pos, oldBlock, blockstate1);
            }

            if ((j & 2) != 0 && (!this.isClientSide || (j & 4) == 0) && (this.isClientSide || levelchunk == null || levelchunk.getFullStatus() != null && levelchunk.getFullStatus().isOrAfter(FullChunkStatus.BLOCK_TICKING))) {
                this.sendBlockUpdated(pos, oldBlock, newBlock, j);
            }

            if ((j & 1) != 0) {
                this.blockUpdated(pos, oldBlock.getBlock());
                if (!this.isClientSide && newBlock.hasAnalogOutputSignal()) {
                    this.updateNeighbourForOutputSignal(pos, block);
                }
            }

            if ((j & 16) == 0 && k > 0) {
                int i = j & -34;
                oldBlock.updateIndirectNeighbourShapes(this, pos, i, k - 1);
                try {
                    if (this.world != null) {
                        BlockPhysicsEvent event = new BlockPhysicsEvent(CraftBlock.at(this, pos), CraftBlockData.fromData(newBlock));
                        Bukkit.getPluginManager().callEvent(event);
                        if (event.isCancelled()) {
                            return;
                        }
                    }
                } catch (StackOverflowError e) {
                    lastPhysicsProblem = pos;
                }
                newBlock.updateNeighbourShapes(this, pos, i, k - 1);
                newBlock.updateIndirectNeighbourShapes(this, pos, i, k - 1);
            }

            if (!this.preventPoiUpdated) {
                this.onBlockStateChange(pos, oldBlock, blockstate1);
            }
        }
    }

    public CraftServer getCraftServer() {
        return (CraftServer) Bukkit.getServer();
    }

    public CraftWorld getWorld() {
        if (this.world == null) {
            Optional<Field> delegate = WrappedWorlds.getDelegate(this.getClass());
            if (delegate.isPresent()) {
                try {
                    return ((WorldBridge) delegate.get().get(this)).bridge$getWorld();
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
            if (environment == null) {
                environment = ArclightServer.getEnvironment(this.getTypeKey());
            }
            if (generator == null) {
                generator = getCraftServer().getGenerator(((ServerLevelData) this.getLevelData()).getLevelName());
                if (generator != null && (Object) this instanceof ServerLevel serverWorld) {
                    org.bukkit.generator.WorldInfo worldInfo = new CraftWorldInfo((ServerLevelData) getLevelData(),
                        ((ServerWorldBridge) this).bridge$getConvertable(), environment, this.dimensionType());
                    if (biomeProvider == null && generator != null) {
                        biomeProvider = generator.getDefaultBiomeProvider(worldInfo);
                    }
                    var generator = serverWorld.getChunkSource().getGenerator();
                    if (biomeProvider != null) {
                        BiomeSource biomeSource = new CustomWorldChunkManager(worldInfo, biomeProvider, serverWorld.registryAccess().registryOrThrow(Registries.BIOME));
                        ((ChunkGeneratorBridge) generator).bridge$setBiomeSource(biomeSource);
                    }
                    CustomChunkGenerator gen = new CustomChunkGenerator(serverWorld, generator, this.generator);
                    ((ServerChunkProviderBridge) serverWorld.getChunkSource()).bridge$setChunkGenerator(gen);
                }
            }
            this.world = new CraftWorld((ServerLevel) (Object) this, generator, biomeProvider, environment);
            getCraftServer().addWorld(this.world);
        }
        return this.world;
    }

    public BlockEntity getBlockEntity(BlockPos pos, boolean validate) {
        return getBlockEntity(pos);
    }

    @Override
    public BlockEntity bridge$getTileEntity(BlockPos pos, boolean validate) {
        return getBlockEntity(pos, validate);
    }

    @Override
    public CraftServer bridge$getServer() {
        return (CraftServer) Bukkit.getServer();
    }

    @Override
    public CraftWorld bridge$getWorld() {
        return this.getWorld();
    }

    @Override
    public boolean bridge$isPvpMode() {
        return this.pvpMode;
    }

    @Override
    public boolean bridge$isKeepSpawnInMemory() {
        return this.keepSpawnInMemory;
    }

    @Override
    public boolean bridge$isPopulating() {
        return this.populating;
    }

    @Override
    public void bridge$setPopulating(boolean populating) {
        this.populating = populating;
    }

    @Override
    public ChunkGenerator bridge$getGenerator() {
        return generator;
    }

    @Override
    public ServerLevel bridge$getMinecraftWorld() {
        return getWorld().getHandle();
    }

    @Override
    public boolean bridge$addEntity(Entity entity, CreatureSpawnEvent.SpawnReason reason) {
        if (getWorld().getHandle() != (Object) this) {
            return ((WorldBridge) getWorld().getHandle()).bridge$addEntity(entity, reason);
        } else {
            this.bridge$pushAddEntityReason(reason);
            return this.addFreshEntity(entity);
        }
    }

    @Override
    public void bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason reason) {
        if (getWorld().getHandle() != (Object) this) {
            ((WorldBridge) getWorld().getHandle()).bridge$pushAddEntityReason(reason);
        }
    }

    @Override
    public CreatureSpawnEvent.SpawnReason bridge$getAddEntityReason() {
        if (getWorld().getHandle() != (Object) this) {
            return ((WorldBridge) getWorld().getHandle()).bridge$getAddEntityReason();
        }
        return null;
    }

    @Override
    public boolean bridge$preventPoiUpdated() {
        return this.preventPoiUpdated;
    }

    @Override
    public void bridge$preventPoiUpdated(boolean b) {
        this.preventPoiUpdated = b;
    }

    private transient Explosion.BlockInteraction arclight$blockInteractionOverride;

    @ModifyVariable(method = "explode(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;Lnet/minecraft/world/level/ExplosionDamageCalculator;DDDFZLnet/minecraft/world/level/Level$ExplosionInteraction;ZLnet/minecraft/core/particles/ParticleOptions;Lnet/minecraft/core/particles/ParticleOptions;Lnet/minecraft/sounds/SoundEvent;)Lnet/minecraft/world/level/Explosion;",
        ordinal = 0, at = @At("HEAD"), argsOnly = true)
    private Level.ExplosionInteraction arclight$standardExplodePre(Level.ExplosionInteraction interaction) {
        if (interaction == ArclightConstants.STANDARD) {
            arclight$blockInteractionOverride = Explosion.BlockInteraction.DESTROY;
            return Level.ExplosionInteraction.BLOCK;
        }
        return interaction;
    }

    @ModifyVariable(method = "explode(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;Lnet/minecraft/world/level/ExplosionDamageCalculator;DDDFZLnet/minecraft/world/level/Level$ExplosionInteraction;ZLnet/minecraft/core/particles/ParticleOptions;Lnet/minecraft/core/particles/ParticleOptions;Lnet/minecraft/sounds/SoundEvent;)Lnet/minecraft/world/level/Explosion;",
        at = @At(value = "LOAD", ordinal = 0))
    private Explosion.BlockInteraction arclight$standardExplodePost(Explosion.BlockInteraction interaction) {
        try {
            if (arclight$blockInteractionOverride != null) {
                return arclight$blockInteractionOverride;
            } else {
                return interaction;
            }
        } finally {
            arclight$blockInteractionOverride = null;
        }
    }
}
