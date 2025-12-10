package io.izzel.arclight.common.mixin.core.server.level;

import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import io.izzel.arclight.common.bridge.bukkit.CraftServerBridge;
import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import io.izzel.arclight.common.bridge.core.entity.player.ServerPlayerEntityBridge;
import io.izzel.arclight.common.bridge.core.inventory.IInventoryBridge;
import io.izzel.arclight.common.bridge.core.server.MinecraftServerBridge;
import io.izzel.arclight.common.bridge.core.world.ExplosionBridge;
import io.izzel.arclight.common.bridge.core.world.IWorldBridge;
import io.izzel.arclight.common.bridge.core.world.level.levelgen.flat.FlatLevelGeneratorSettingsBridge;
import io.izzel.arclight.common.bridge.core.world.server.ServerChunkProviderBridge;
import io.izzel.arclight.common.bridge.core.world.server.ServerWorldBridge;
import io.izzel.arclight.common.bridge.core.world.storage.DerivedWorldInfoBridge;
import io.izzel.arclight.common.bridge.core.world.storage.LevelStorageSourceBridge;
import io.izzel.arclight.common.bridge.core.world.storage.MapDataBridge;
import io.izzel.arclight.common.bridge.core.world.storage.WorldInfoBridge;
import io.izzel.arclight.common.mixin.core.world.level.LevelMixin;
import io.izzel.arclight.common.mod.mixins.annotation.CreateConstructor;
import io.izzel.arclight.common.mod.mixins.annotation.ShadowConstructor;
import io.izzel.arclight.common.mod.server.ArclightServer;
import io.izzel.arclight.common.mod.server.entity.ArclightSpawnReason;
import io.izzel.arclight.common.mod.server.event.ArclightEventFactory;
import io.izzel.arclight.common.mod.server.world.LevelPersistentData;
import io.izzel.arclight.common.mod.server.world.WorldSymlink;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import io.izzel.arclight.common.mod.util.DelegateWorldInfo;
import io.izzel.arclight.common.mod.util.DistValidate;
import io.izzel.arclight.i18n.ArclightConfig;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.izzel.arclight.mixin.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.util.ProgressListener;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.RandomSequences;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.CustomSpawner;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.dimension.end.EndDragonFight;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.craftbukkit.v.block.CraftBlockState;
import org.bukkit.craftbukkit.v.entity.CraftHumanEntity;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.craftbukkit.v.generator.CustomChunkGenerator;
import org.bukkit.craftbukkit.v.generator.CustomWorldChunkManager;
import org.bukkit.craftbukkit.v.util.CraftNamespacedKey;
import org.bukkit.craftbukkit.v.util.WorldUUID;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.LightningStrike;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.server.MapInitializeEvent;
import org.bukkit.event.weather.LightningStrikeEvent;
import org.bukkit.event.world.GenericGameEvent;
import org.bukkit.event.world.TimeSkipEvent;
import org.bukkit.event.world.WorldSaveEvent;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spigotmc.SpigotWorldConfig;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nonnull;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin extends LevelMixin implements ServerWorldBridge {

    // @formatter:off
    @Shadow public abstract boolean addFreshEntity(Entity entityIn);
    @Shadow public abstract boolean addWithUUID(Entity entityIn);
    @Shadow public abstract <T extends ParticleOptions> int sendParticles(T type, double posX, double posY, double posZ, int particleCount, double xOffset, double yOffset, double zOffset, double speed);
    @Shadow protected abstract boolean sendParticles(ServerPlayer player, boolean longDistance, double posX, double posY, double posZ, Packet<?> packet);
    @Shadow @Nonnull public abstract MinecraftServer getServer();
    @Shadow @Final private List<ServerPlayer> players;
    @Shadow public abstract ServerChunkCache getChunkSource();
    @Shadow protected abstract void wakeUpAllPlayers();
    @Shadow @Final private ServerChunkCache chunkSource;
    @Shadow @Final public static BlockPos END_SPAWN_POINT;
    @Shadow @Final public ServerLevelData serverLevelData;
    @Shadow @Final private PersistentEntitySectionManager<Entity> entityManager;
    @Shadow public abstract DimensionDataStorage getDataStorage();
    @Shadow protected abstract void addPlayer(ServerPlayer serverPlayer);
    @Shadow @Nullable public abstract Entity getEntity(int i);
    @Shadow public abstract void sendBlockUpdated(BlockPos blockPos, BlockState blockState, BlockState blockState2, int i);
    @Shadow @javax.annotation.Nullable private EndDragonFight dragonFight;
    // @formatter:on

    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    public PrimaryLevelData K; // Stupid CraftBukkit patch.
    public LevelStorageSource.LevelStorageAccess convertable;
    public UUID uuid;
    public ResourceKey<LevelStem> typeKey;

    @Override
    public ResourceKey<LevelStem> getTypeKey() {
        return this.typeKey;
    }

    @ShadowConstructor
    public void arclight$constructor(MinecraftServer minecraftServer, Executor backgroundExecutor, LevelStorageSource.LevelStorageAccess levelSave, ServerLevelData worldInfo, ResourceKey<Level> dimension, LevelStem levelStem, ChunkProgressListener statusListener, boolean isDebug, long seed, List<CustomSpawner> specialSpawners, boolean shouldBeTicking, RandomSequences seq) {
        throw new RuntimeException();
    }

    @CreateConstructor
    public void arclight$constructor(MinecraftServer server, Executor backgroundExecutor, LevelStorageSource.LevelStorageAccess levelSave, PrimaryLevelData worldInfo, ResourceKey<Level> dimension, LevelStem levelStem, ChunkProgressListener statusListener, boolean isDebug, long seed, List<CustomSpawner> specialSpawners, boolean shouldBeTicking, RandomSequences seq, org.bukkit.World.Environment env, org.bukkit.generator.ChunkGenerator gen, org.bukkit.generator.BiomeProvider biomeProvider) {
        var craftBridge = (CraftServerBridge)(Object) ((MinecraftServerBridge) server).bridge$getServer();
        assert craftBridge != null; // Already checked in bridge
        // We have no way but store it somewhere and use a default value
        // in order to avoid having to pass them as arguments.
        craftBridge.bridge$offerEnvironmentCache(worldInfo.getLevelName(), env);
        craftBridge.bridge$offerGeneratorCache(worldInfo.getLevelName(), gen);
        craftBridge.bridge$offerBiomeProviderCache(worldInfo.getLevelName(), biomeProvider);
        arclight$constructor(server, backgroundExecutor, levelSave, worldInfo, dimension, levelStem, statusListener, isDebug, seed, specialSpawners, shouldBeTicking, seq);
        bridge$getWorld();
    }

    // Support custom chunk generator; in consistency with CraftBukkit
    // The real part is inside ServerChunkCache, when initializing ChunkMap (in ctor).
    // A generator state is created, which is later used for chunk generation.
    // Previously we didn't modify it before ChunkMap is created,
    // which in turn cause custom world generation from Bukkit failing to work.
    @Decorate(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/dimension/LevelStem;generator()Lnet/minecraft/world/level/chunk/ChunkGenerator;"))
    private ChunkGenerator arclight$initChunkGenerator(LevelStem instance, @Local(ordinal = -1) MinecraftServer server, @Local(ordinal = -1) ServerLevelData worldInfo) throws Throwable {
        // Pulling up world info init since level info is used when selecting ChunkGenerator.
        if (worldInfo instanceof PrimaryLevelData primary) {
            this.K = primary;
        } else {
            // damn spigot again
            this.K = DelegateWorldInfo.wrap(worldInfo);
        }

        var craftBridge = (CraftServerBridge) (Object) ((MinecraftServerBridge) server).bridge$getServer();

        this.biomeProvider = craftBridge.bridge$consumeBiomeProviderCache(worldInfo.getLevelName());
        this.generator = craftBridge.bridge$consumeGeneratorCache(worldInfo.getLevelName());
        this.environment = craftBridge.bridge$consumeEnvironmentCache(worldInfo.getLevelName());

        if (this.environment == null) {
            // Select world environment for vanilla/mod world creation
            if (instance.type().is(LevelStem.OVERWORLD.location())) {
                this.environment = World.Environment.NORMAL;
            } else if (instance.type().is(LevelStem.NETHER.location())) {
                this.environment = World.Environment.NETHER;
            } else if (instance.type().is(LevelStem.END.location())) {
                this.environment = World.Environment.THE_END;
            } else {
                // Don't use CUSTOM; it's not even supported in Multiverse
                // this.environment = World.Environment.CUSTOM;
                this.environment = World.Environment.NORMAL;
            }
        }

        // Data needed by getWorld() are all initialized for possible creating CraftWorld.
        // CraftBukkit start: select custom chunk generator
        ChunkGenerator raw = (ChunkGenerator) DecorationOps.callsite().invoke(instance);
        if (biomeProvider != null) {
            BiomeSource biomeSource = new CustomWorldChunkManager(getWorld(), biomeProvider, getServer().registryAccess().registryOrThrow(Registries.BIOME));
            if (raw instanceof NoiseBasedChunkGenerator noise) {
                raw = new NoiseBasedChunkGenerator(biomeSource, noise.settings);
            } else if (raw instanceof FlatLevelSource flat) {
                raw = new FlatLevelSource(((FlatLevelGeneratorSettingsBridge)flat.settings()).bridge$withBiomeSource(biomeSource));
            } else {
                ArclightServer.LOGGER.warn("Level {} has unknown customized generator -- requested biome provider won't be satisfied.", this.serverLevelData.getLevelName());
            }
        }
        if (generator != null) {
            raw = new CustomChunkGenerator((ServerLevel)(Object) this, raw, generator);
        }
        // CraftBukkit end
        return raw;
    }

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;getWorldData()Lnet/minecraft/world/level/storage/WorldData;"))
    private WorldData arclight$useRespective(MinecraftServer server) {
        return K;
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void arclight$init(MinecraftServer minecraftServer, Executor backgroundExecutor, LevelStorageSource.LevelStorageAccess levelSave, ServerLevelData worldInfo, ResourceKey<Level> dimension, LevelStem levelStem, ChunkProgressListener statusListener, boolean isDebug, long seed, List<CustomSpawner> specialSpawners, boolean shouldBeTicking, RandomSequences seq, CallbackInfo ci) {
        this.pvpMode = minecraftServer.isPvpAllowed();
        this.convertable = levelSave;
        if (this.dragonFight == null && this.environment == World.Environment.THE_END) {
            this.dragonFight = new EndDragonFight((ServerLevel)(Object) this, K.worldGenOptions().seed(), K.endDragonFightData());
        }
        var typeKey = ((LevelStorageSourceBridge.LevelStorageAccessBridge) levelSave).bridge$getTypeKey();
        if (typeKey != null) {
            this.typeKey = typeKey;
        } else {
            var dimensions = getServer().registryAccess().registryOrThrow(Registries.LEVEL_STEM);
            var key = dimensions.getResourceKey(levelStem);
            if (key.isPresent()) {
                this.typeKey = key.get();
            } else {
                ArclightServer.LOGGER.warn("Assign {} to unknown level stem {}", dimension.location(), levelStem);
                this.typeKey = ResourceKey.create(Registries.LEVEL_STEM, dimension.location());
            }
            if (worldInfo instanceof DerivedLevelData data) {
                ((DerivedWorldInfoBridge) worldInfo).bridge$setDimType(this.getTypeKey());
                if (ArclightConfig.spec().getCompat().isSymlinkWorld()) {
                    WorldSymlink.create(data, levelSave.getDimensionPath(this.dimension()).toFile());
                }
            }
        }
        this.spigotConfig = new SpigotWorldConfig(worldInfo.getLevelName());
        this.uuid = WorldUUID.getUUID(levelSave.getDimensionPath(this.dimension()).toFile());
        ((ServerChunkProviderBridge) this.chunkSource).bridge$setViewDistance(spigotConfig.viewDistance);
        ((ServerChunkProviderBridge) this.chunkSource).bridge$setSimulationDistance(spigotConfig.simulationDistance);
        ((WorldInfoBridge) this.K).bridge$setWorld((ServerLevel) (Object) this);
        var data = this.getDataStorage().computeIfAbsent(LevelPersistentData.factory(), "bukkit_pdc");
        this.bridge$getWorld().readBukkitValues(data.getTag());
    }

    @Inject(method = "saveLevelData", at = @At("RETURN"))
    private void arclight$savePdc(CallbackInfo ci) {
        var data = this.getDataStorage().computeIfAbsent(LevelPersistentData.factory(), "bukkit_pdc");
        data.save(this.world);
    }

    @Inject(method = "gameEvent", cancellable = true, at = @At("HEAD"))
    private void arclight$gameEventEvent(Holder<GameEvent> holder, Vec3 pos, GameEvent.Context context, CallbackInfo ci) {
        var entity = context.sourceEntity();
        var i = holder.value().notificationRadius();
        GenericGameEvent event = new GenericGameEvent(org.bukkit.GameEvent.getByKey(CraftNamespacedKey.fromMinecraft(BuiltInRegistries.GAME_EVENT.getKey(holder.value()))), new Location(this.getWorld(), pos.x(), pos.y(), pos.z()), (entity == null) ? null : entity.bridge$getBukkitEntity(), i, !Bukkit.isPrimaryThread());
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            ci.cancel();
        }
    }

    public LevelChunk getChunkIfLoaded(int x, int z) {
        return this.chunkSource.getChunk(x, z, false);
    }

    public <T extends ParticleOptions> int sendParticles(final ServerPlayer sender, final T t0, final double d0, final double d1, final double d2, final int i, final double d3, final double d4, final double d5, final double d6, final boolean force) {
        ClientboundLevelParticlesPacket packet = new ClientboundLevelParticlesPacket(t0, force, d0, d1, d2, (float) d3, (float) d4, (float) d5, (float) d6, i);
        int j = 0;
        for (ServerPlayer entity : this.players) {
            if (sender == null || ((ServerPlayerEntityBridge) entity).bridge$getBukkitEntity().canSee(((ServerPlayerEntityBridge) sender).bridge$getBukkitEntity())) {
                if (this.sendParticles(entity, force, d0, d1, d2, packet)) {
                    ++j;
                }
            }
        }
        return j;
    }

    @Override
    public LevelStorageSource.LevelStorageAccess bridge$getConvertable() {
        return this.convertable;
    }

    @Inject(method = "tickNonPassenger", at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/world/entity/Entity;tick()V"))
    private void arclight$tickPortal(Entity entityIn, CallbackInfo ci) {
        ((EntityBridge) entityIn).bridge$postTick();
    }

    @Inject(method = "tickPassenger", at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/world/entity/Entity;rideTick()V"))
    private void arclight$tickPortalPassenger(Entity ridingEntity, Entity passengerEntity, CallbackInfo ci) {
        ((EntityBridge) passengerEntity).bridge$postTick();
    }

    @Decorate(method = "tickChunk", at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/util/RandomSource;nextInt(I)I"))
    private int arclight$modifyThunderChance(RandomSource instance, int i) throws Throwable {
        return (int) DecorationOps.callsite().invoke(instance, i == 100000 ? spigotConfig.thunderChance : i);
    }

    @Inject(method = "tickChunk", at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/server/level/ServerLevel;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private void arclight$spawnReasonForSkeletonHorse(LevelChunk levelChunk, int i, CallbackInfo ci) {
        bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.LIGHTNING);
    }

    @Decorate(method = "tickChunk", at = @At(value = "INVOKE", ordinal = 1, target = "Lnet/minecraft/server/level/ServerLevel;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean arclight$spawnReasonForLightning(ServerLevel instance, Entity entity) throws Throwable {
        if (DistValidate.isValid(this)) {
            LightningStrikeEvent lightning = CraftEventFactory.callLightningStrikeEvent((LightningStrike) entity.bridge$getBukkitEntity(), LightningStrikeEvent.Cause.WEATHER);
            if (lightning.isCancelled()) {
                return false;
            }
        }
        return (boolean) DecorationOps.callsite().invoke(instance, entity);
    }

    public boolean strikeLightning(Entity entity) {
        return this.strikeLightning(entity, LightningStrikeEvent.Cause.UNKNOWN);
    }

    public boolean strikeLightning(Entity entity, LightningStrikeEvent.Cause cause) {
        if (arclight$cause != null) {
            cause = arclight$cause;
            arclight$cause = null;
        }
        if (DistValidate.isValid(this)) {
            LightningStrikeEvent lightning = CraftEventFactory.callLightningStrikeEvent((LightningStrike) entity.bridge$getBukkitEntity(), cause);
            if (lightning.isCancelled()) {
                return false;
            }
        }
        return this.addFreshEntity(entity);
    }

    @Decorate(method = "tickPrecipitation", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    public boolean arclight$snowForm(ServerLevel level, BlockPos pos, BlockState newState) throws Throwable {
        final var event = ArclightEventFactory.callBlockFormEvent(level, pos, newState, 3, null);
        if (event != null) {
            if (event.isCancelled()) {
                return false;
            }
            newState = ((CraftBlockState) event.getNewState()).getHandle();
        }
        return (boolean) DecorationOps.callsite().invoke(level, pos, newState);
    }

    //TODO: weather cycle for every player

    @Inject(method = "save", at = @At(value = "JUMP", ordinal = 0, opcode = Opcodes.IFNULL))
    private void arclight$worldSaveEvent(ProgressListener progress, boolean flush, boolean skipSave, CallbackInfo ci) {
        if (DistValidate.isValid(this)) {
            Bukkit.getPluginManager().callEvent(new WorldSaveEvent(bridge$getWorld()));
        }
    }

    @Inject(method = "save", at = @At("RETURN"))
    private void arclight$saveLevelDat(ProgressListener progress, boolean flush, boolean skipSave, CallbackInfo ci) {
        if (this.serverLevelData instanceof PrimaryLevelData worldInfo) {
            worldInfo.setWorldBorder(this.getWorldBorder().createSettings());
            worldInfo.setCustomBossEvents(this.getServer().getCustomBossEvents().save(this.registryAccess()));
            this.convertable.saveDataTag(this.getServer().registryAccess(), worldInfo, this.getServer().getPlayerList().getSingleplayerData());
        }
    }

    // Multiworld support: use respective world data
    @Decorate(method = {"saveLevelData", "findNearestMapStructure", "isFlat", "getSeed"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;getWorldData()Lnet/minecraft/world/level/storage/WorldData;"))
    private WorldData arclight$findNearestMapStructure(MinecraftServer instance) throws Throwable {
        return serverLevelData instanceof PrimaryLevelData primary ? primary : (WorldData) DecorationOps.callsite().invoke(instance);
    }

    @Inject(method = "unload", at = @At("HEAD"))
    public void arclight$closeOnChunkUnloading(LevelChunk chunkIn, CallbackInfo ci) {
        for (BlockEntity tileentity : chunkIn.getBlockEntities().values()) {
            if (tileentity instanceof Container) {
                for (HumanEntity h : Lists.newArrayList(((IInventoryBridge) tileentity).getViewers())) {
                    if (h instanceof CraftHumanEntity) {
                        ((CraftHumanEntity) h).getHandle().closeContainer();
                    }
                }
            }
        }
    }

    private transient boolean arclight$force = false;

    @Decorate(method = "sendParticles(Lnet/minecraft/core/particles/ParticleOptions;DDDIDDDD)I", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;sendParticles(Lnet/minecraft/server/level/ServerPlayer;ZDDDLnet/minecraft/network/protocol/Packet;)Z"))
    public boolean arclight$particleVisible(ServerLevel serverWorld, ServerPlayer player, boolean longDistance, double posX, double posY, double posZ, Packet<?> packet) throws Throwable {
        try {
            return (boolean) DecorationOps.callsite().invoke(serverWorld, player, arclight$force, posX, posY, posZ, packet);
        } finally {
            arclight$force = false;
        }
    }

    public <T extends ParticleOptions> int sendParticles(T type, double posX, double posY, double posZ, int particleCount, double xOffset, double yOffset, double zOffset, double speed, boolean force) {
        arclight$force = force;
        return this.sendParticles(type, posX, posY, posZ, particleCount, xOffset, yOffset, zOffset, speed);
    }

    @Override
    public <T extends ParticleOptions> int bridge$sendParticles(T type, double posX, double posY, double posZ, int particleCount, double xOffset, double yOffset, double zOffset, double speed, boolean force) {
        return this.sendParticles(type, posX, posY, posZ, particleCount, xOffset, yOffset, zOffset, speed, force);
    }

    private transient LightningStrikeEvent.Cause arclight$cause;

    @Override
    public void bridge$pushStrikeLightningCause(LightningStrikeEvent.Cause cause) {
        this.arclight$cause = cause;
    }

    @Override
    public void bridge$strikeLightning(LightningBolt entity, LightningStrikeEvent.Cause cause) {
        strikeLightning(entity, cause);
    }

    private transient CreatureSpawnEvent.SpawnReason arclight$reason;
    // Fix for SPIGOT-6415
    private transient ArclightSpawnReason arclight$extendedReason;

    @Inject(method = "addEntity", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/entity/PersistentEntitySectionManager;addNewEntity(Lnet/minecraft/world/level/entity/EntityAccess;)Z"))
    private void arclight$addEntityEvent(Entity entityIn, CallbackInfoReturnable<Boolean> cir) {
        // SPIGOT-6415: Don't call spawn event when reason is null.
        final var reason = arclight$reason;
        arclight$reason = null;
        if (arclight$extendedReason == ArclightSpawnReason.TELEPORT && reason == null) {
            return;
        }
        CreatureSpawnEvent.SpawnReason spawnReason = reason == null ? CreatureSpawnEvent.SpawnReason.DEFAULT : reason;
        if (!DistValidate.isValid(this)) {
            return;
        }
        ((EntityBridge) entityIn).arclight$onAddedToLevel();
        if (!CraftEventFactory.doEntityAddEventCalling((ServerLevel) (Object) this, entityIn, spawnReason)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "addEntity", at = @At("RETURN"))
    public void arclight$resetReason(Entity entityIn, CallbackInfoReturnable<Boolean> cir) {
        arclight$reason = null;
    }

    @Override
    public void bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason reason) {
        this.arclight$reason = reason;
    }

    @Override
    public CreatureSpawnEvent.SpawnReason bridge$getAddEntityReason() {
        return this.arclight$reason;
    }

    public boolean addFreshEntity(Entity entity, CreatureSpawnEvent.SpawnReason reason) {
        bridge$pushAddEntityReason(reason);
        return addFreshEntity(entity);
    }

    @Override
    public boolean bridge$addEntity(Entity entity, CreatureSpawnEvent.SpawnReason reason) {
        return addFreshEntity(entity, reason);
    }

    public boolean addWithUUID(Entity entity, CreatureSpawnEvent.SpawnReason reason) {
        bridge$pushAddEntityReason(reason);
        return addWithUUID(entity);
    }

    @Inject(method = "addDuringTeleport", at = @At("HEAD"))
    private void arclight$ignoreSpawnOnTeleport(Entity entity, CallbackInfo ci) {
        arclight$extendedReason = ArclightSpawnReason.TELEPORT;
    }

    @Inject(method = "addDuringTeleport", at = @At("RETURN"))
    private void arclight$unsetIgnoreSpawnOnTeleport(Entity entity, CallbackInfo ci) {
        arclight$extendedReason = null;
    }

    public void addDuringTeleport(Entity entity, CreatureSpawnEvent.SpawnReason reason) {
        if (entity instanceof ServerPlayer player) {
            this.addPlayer(player);
        } else if (reason != null) {
            this.addFreshEntity(entity, reason);
        } else {
            arclight$extendedReason = ArclightSpawnReason.TELEPORT;
            addFreshEntity(entity);
            arclight$extendedReason = null;
        }
    }

    @Override
    public boolean bridge$addEntitySerialized(Entity entity, CreatureSpawnEvent.SpawnReason reason) {
        return addWithUUID(entity, reason);
    }

    public boolean tryAddFreshEntityWithPassengers(Entity entity, CreatureSpawnEvent.SpawnReason reason) {
        if (entity.getSelfAndPassengers().map(Entity::getUUID).anyMatch(this.entityManager::isLoaded)) {
            return false;
        }
        return this.bridge$addAllEntities(entity, reason);
    }

    @Override
    public boolean bridge$addAllEntitiesSafely(Entity entity, CreatureSpawnEvent.SpawnReason reason) {
        return tryAddFreshEntityWithPassengers(entity, reason);
    }

    @Decorate(method = "destroyBlockProgress", at = @At(value = "INVOKE", target = "Ljava/util/List;iterator()Ljava/util/Iterator;"))
    private Iterator<ServerPlayer> arclight$limitBreakVisibility(List<ServerPlayer> instance, @Local(ordinal = 0) int id) throws Throwable {
        final var raw = (Iterator<ServerPlayer>) DecorationOps.callsite().invoke(instance);
        final var actor = getEntity(id);
        if (!(actor instanceof ServerPlayerEntityBridge player)) {
            return raw;
        }
        return Iterators.filter(raw, it -> it != null && ((ServerPlayerEntityBridge)it).bridge$getBukkitEntity().canSee(player.bridge$getBukkitEntity()));
    }

    @Decorate(method = "sendBlockUpdated", at = @At(value = "INVOKE", target = "Ljava/util/Iterator;next()Ljava/lang/Object;"))
    private Object arclight$guardConcurrencyOnNavigation(Iterator<PathNavigation> instance, @Local(ordinal = 0) BlockPos pos, @Local(ordinal = 0) BlockState before, @Local(ordinal = 1) BlockState after, @Local(ordinal = 0) int i) throws Throwable {
        try {
            return DecorationOps.callsite().invoke(instance);
        } catch (ConcurrentModificationException ignored) {
            sendBlockUpdated(pos,  before, after, i);
            return DecorationOps.cancel().invoke();
        }
    }

    @Decorate(method = "explode", inject = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Explosion;interactsWithBlocks()Z"))
    private void arclight$doExplosion(@Local(ordinal = -1) Explosion explosion) throws Throwable {
        if (((ExplosionBridge) explosion).bridge$wasCancelled()) {
            DecorationOps.cancel().invoke(explosion);
            return;
        }
        DecorationOps.blackhole().invoke();
    }

    @Inject(method = "getMapData", at = @At("RETURN"))
    private void arclight$mapSetId(MapId id, CallbackInfoReturnable<MapItemSavedData> cir) {
        var data = cir.getReturnValue();
        if (data != null) {
            ((MapDataBridge) data).bridge$setId(id);
        }
    }

    @Inject(method = "setMapData", at = @At("HEAD"))
    private void arclight$mapSetId(MapId id, MapItemSavedData data, CallbackInfo ci) {
        ((MapDataBridge) data).bridge$setId(id);
        MapInitializeEvent event = new MapInitializeEvent(((MapDataBridge) data).bridge$getMapView());
        Bukkit.getServer().getPluginManager().callEvent(event);
    }

    @Inject(method = "blockUpdated", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;updateNeighborsAt(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/Block;)V"))
    private void arclight$returnIfPopulate(BlockPos pos, Block block, CallbackInfo ci) {
        if (populating) {
            ci.cancel();
        }
    }

    @Override
    public BlockEntity getBlockEntity(BlockPos pos, boolean validate) {
        return this.getBlockEntity(pos);
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;setDayTime(J)V"))
    private void arclight$timeSkip(ServerLevel world, long time) {
        TimeSkipEvent event = new TimeSkipEvent(this.bridge$getWorld(), TimeSkipEvent.SkipReason.NIGHT_SKIP, (time - time % 24000L) - this.getDayTime());
        Bukkit.getPluginManager().callEvent(event);
        arclight$timeSkipCancelled = event.isCancelled();
        if (!event.isCancelled()) {
            world.setDayTime(this.getDayTime() + event.getSkipAmount());
        }
    }

    private transient boolean arclight$timeSkipCancelled;

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;wakeUpAllPlayers()V"))
    private void arclight$notWakeIfCancelled(ServerLevel world) {
        if (!arclight$timeSkipCancelled) {
            this.wakeUpAllPlayers();
        }
        arclight$timeSkipCancelled = false;
    }

    @Override
    public ServerLevel bridge$getMinecraftWorld() {
        return (ServerLevel) (Object) this;
    }

    @ModifyVariable(method = "tickBlock", ordinal = 0, argsOnly = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;tick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)V"))
    private BlockPos arclight$captureTickingBlock(BlockPos pos) {
        ArclightCaptures.captureTickingBlock((ServerLevel) (Object) this, pos);
        return pos;
    }

    @ModifyVariable(method = "tickBlock", ordinal = 0, argsOnly = true, at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/world/level/block/state/BlockState;tick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)V"))
    private BlockPos arclight$resetTickingBlock(BlockPos pos) {
        ArclightCaptures.resetTickingBlock();
        return pos;
    }

    @ModifyVariable(method = "tickChunk", ordinal = 0, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;randomTick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)V"))
    private BlockPos arclight$captureRandomTick(BlockPos pos) {
        ArclightCaptures.captureTickingBlock((ServerLevel) (Object) this, pos);
        return pos;
    }

    @ModifyVariable(method = "tickChunk", ordinal = 0, at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/world/level/block/state/BlockState;randomTick(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)V"))
    private BlockPos arclight$resetRandomTick(BlockPos pos) {
        ArclightCaptures.resetTickingBlock();
        return pos;
    }

    @ModifyVariable(method = "tickNonPassenger", argsOnly = true, ordinal = 0, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;tick()V"))
    private Entity arclight$captureTickingEntity(Entity entity) {
        ArclightCaptures.captureTickingEntity(entity);
        return entity;
    }

    @ModifyVariable(method = "tickNonPassenger", argsOnly = true, ordinal = 0, at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/world/entity/Entity;tick()V"))
    private Entity arclight$resetTickingEntity(Entity entity) {
        ArclightCaptures.resetTickingEntity();
        return entity;
    }

    @ModifyVariable(method = "tickPassenger", argsOnly = true, ordinal = 1, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;rideTick()V"))
    private Entity arclight$captureTickingPassenger(Entity entity) {
        ArclightCaptures.captureTickingEntity(entity);
        return entity;
    }

    @ModifyVariable(method = "tickPassenger", argsOnly = true, ordinal = 1, at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/world/entity/Entity;rideTick()V"))
    private Entity arclight$resetTickingPassenger(Entity entity) {
        ArclightCaptures.resetTickingEntity();
        return entity;
    }
}
