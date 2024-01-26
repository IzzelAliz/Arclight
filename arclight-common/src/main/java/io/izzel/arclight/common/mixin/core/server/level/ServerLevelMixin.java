package io.izzel.arclight.common.mixin.core.server.level;

import com.google.common.collect.Lists;
import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import io.izzel.arclight.common.bridge.core.entity.player.ServerPlayerEntityBridge;
import io.izzel.arclight.common.bridge.core.inventory.IInventoryBridge;
import io.izzel.arclight.common.bridge.core.world.ExplosionBridge;
import io.izzel.arclight.common.bridge.core.world.WorldBridge;
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
import io.izzel.arclight.common.mod.server.world.LevelPersistentData;
import io.izzel.arclight.common.mod.server.world.WorldSymlink;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import io.izzel.arclight.common.mod.util.DelegateWorldInfo;
import io.izzel.arclight.common.mod.util.DistValidate;
import io.izzel.arclight.i18n.ArclightConfig;
import net.minecraft.core.BlockPos;
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
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.ProgressListener;
import net.minecraft.world.Container;
import net.minecraft.world.RandomSequences;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.CustomSpawner;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.level.storage.DerivedLevelData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.PrimaryLevelData;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v.CraftWorld;
import org.bukkit.craftbukkit.v.entity.CraftHumanEntity;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.craftbukkit.v.generator.CustomChunkGenerator;
import org.bukkit.craftbukkit.v.util.BlockStateListPopulator;
import org.bukkit.craftbukkit.v.util.CraftNamespacedKey;
import org.bukkit.craftbukkit.v.util.WorldUUID;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.LightningStrike;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.server.MapInitializeEvent;
import org.bukkit.event.weather.LightningStrikeEvent;
import org.bukkit.event.world.GenericGameEvent;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.event.world.TimeSkipEvent;
import org.bukkit.event.world.WorldSaveEvent;
import org.objectweb.asm.Opcodes;
import org.spigotmc.SpigotWorldConfig;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import javax.annotation.Nonnull;
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
    // @formatter:on

    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    public PrimaryLevelData K; // TODO f_8549_ check on update
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
    public void arclight$constructor(MinecraftServer minecraftServer, Executor backgroundExecutor, LevelStorageSource.LevelStorageAccess levelSave, PrimaryLevelData worldInfo, ResourceKey<Level> dimension, LevelStem levelStem, ChunkProgressListener statusListener, boolean isDebug, long seed, List<CustomSpawner> specialSpawners, boolean shouldBeTicking, RandomSequences seq, org.bukkit.World.Environment env, org.bukkit.generator.ChunkGenerator gen, org.bukkit.generator.BiomeProvider biomeProvider) {
        arclight$constructor(minecraftServer, backgroundExecutor, levelSave, worldInfo, dimension, levelStem, statusListener, isDebug, seed, specialSpawners, shouldBeTicking, seq);
        this.generator = gen;
        this.environment = env;
        this.biomeProvider = biomeProvider;
        if (gen != null) {
            CustomChunkGenerator generator = new CustomChunkGenerator((ServerLevel) (Object) this, this.chunkSource.getGenerator(), gen);
            ((ServerChunkProviderBridge) this.chunkSource).bridge$setChunkGenerator(generator);
        }
        bridge$getWorld();
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void arclight$init(MinecraftServer minecraftServer, Executor backgroundExecutor, LevelStorageSource.LevelStorageAccess levelSave, ServerLevelData worldInfo, ResourceKey<Level> dimension, LevelStem levelStem, ChunkProgressListener statusListener, boolean isDebug, long seed, List<CustomSpawner> specialSpawners, boolean shouldBeTicking, RandomSequences seq, CallbackInfo ci) {
        this.pvpMode = minecraftServer.isPvpAllowed();
        this.convertable = levelSave;
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
        }
        if (worldInfo instanceof PrimaryLevelData) {
            this.K = (PrimaryLevelData) worldInfo;
        } else if (worldInfo instanceof DerivedLevelData) {
            // damn spigot again
            this.K = DelegateWorldInfo.wrap(((DerivedLevelData) worldInfo));
            ((DerivedWorldInfoBridge) worldInfo).bridge$setDimType(this.getTypeKey());
            if (ArclightConfig.spec().getCompat().isSymlinkWorld()) {
                WorldSymlink.create((DerivedLevelData) worldInfo, levelSave.getDimensionPath(this.dimension()).toFile());
            }
        }
        this.spigotConfig = new SpigotWorldConfig(worldInfo.getLevelName());
        this.uuid = WorldUUID.getUUID(levelSave.getDimensionPath(this.dimension()).toFile());
        ((ServerChunkProviderBridge) this.chunkSource).bridge$setViewDistance(spigotConfig.viewDistance);
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
    private void arclight$gameEventEvent(GameEvent gameEvent, Vec3 pos, GameEvent.Context context, CallbackInfo ci) {
        var entity = context.sourceEntity();
        var i = gameEvent.getNotificationRadius();
        GenericGameEvent event = new GenericGameEvent(org.bukkit.GameEvent.getByKey(CraftNamespacedKey.fromMinecraft(BuiltInRegistries.GAME_EVENT.getKey(gameEvent))), new Location(this.getWorld(), pos.x(), pos.y(), pos.z()), (entity == null) ? null : ((EntityBridge) entity).bridge$getBukkitEntity(), i, !Bukkit.isPrimaryThread());
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

    @Inject(method = "tickChunk", at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/server/level/ServerLevel;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    public void arclight$thunder(LevelChunk chunkIn, int randomTickSpeed, CallbackInfo ci) {
        bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.LIGHTNING);
    }

    @Redirect(method = "tickChunk", at = @At(value = "INVOKE", ordinal = 1, target = "Lnet/minecraft/server/level/ServerLevel;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean arclight$thunder(ServerLevel serverWorld, Entity entityIn) {
        return strikeLightning(entityIn, LightningStrikeEvent.Cause.WEATHER);
    }

    public boolean strikeLightning(Entity entity) {
        return this.strikeLightning(entity, LightningStrikeEvent.Cause.UNKNOWN);
    }

    public boolean strikeLightning(Entity entity, LightningStrikeEvent.Cause cause) {
        if (arclight$cause != null) {
            cause = arclight$cause;
            arclight$cause = null;
        }
        if (DistValidate.isValid((LevelAccessor) this)) {
            LightningStrikeEvent lightning = CraftEventFactory.callLightningStrikeEvent((LightningStrike) ((EntityBridge) entity).bridge$getBukkitEntity(), cause);
            if (lightning.isCancelled()) {
                return false;
            }
        }
        return this.addFreshEntity(entity);
    }

    @Redirect(method = "tickPrecipitation", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"))
    public boolean arclight$snowForm(ServerLevel serverWorld, BlockPos pos, BlockState state) {
        return CraftEventFactory.handleBlockFormEvent(serverWorld, pos, state, null);
    }

    @Inject(method = "save", at = @At(value = "JUMP", ordinal = 0, opcode = Opcodes.IFNULL))
    private void arclight$worldSaveEvent(ProgressListener progress, boolean flush, boolean skipSave, CallbackInfo ci) {
        if (DistValidate.isValid((LevelAccessor) this)) {
            Bukkit.getPluginManager().callEvent(new WorldSaveEvent(bridge$getWorld()));
        }
    }

    @Inject(method = "save", at = @At("RETURN"))
    private void arclight$saveLevelDat(ProgressListener progress, boolean flush, boolean skipSave, CallbackInfo ci) {
        if (this.serverLevelData instanceof PrimaryLevelData worldInfo) {
            worldInfo.setWorldBorder(this.getWorldBorder().createSettings());
            worldInfo.setCustomBossEvents(this.getServer().getCustomBossEvents().save());
            this.convertable.saveDataTag(this.getServer().registryAccess(), worldInfo, this.getServer().getPlayerList().getSingleplayerData());
        }
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

    private transient boolean arclight$force;

    @Redirect(method = "sendParticles(Lnet/minecraft/core/particles/ParticleOptions;DDDIDDDD)I", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;sendParticles(Lnet/minecraft/server/level/ServerPlayer;ZDDDLnet/minecraft/network/protocol/Packet;)Z"))
    public boolean arclight$particleVisible(ServerLevel serverWorld, ServerPlayer player, boolean longDistance, double posX, double posY, double posZ, Packet<?> packet) {
        return this.sendParticles(player, arclight$force, posX, posY, posZ, packet);
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

    @Inject(method = "addEntity", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/entity/PersistentEntitySectionManager;addNewEntity(Lnet/minecraft/world/level/entity/EntityAccess;)Z"))
    private void arclight$addEntityEvent(Entity entityIn, CallbackInfoReturnable<Boolean> cir) {
        CreatureSpawnEvent.SpawnReason reason = arclight$reason == null ? CreatureSpawnEvent.SpawnReason.DEFAULT : arclight$reason;
        arclight$reason = null;
        if (DistValidate.isValid((LevelAccessor) this) && !CraftEventFactory.doEntityAddEventCalling((ServerLevel) (Object) this, entityIn, reason)) {
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

    public void addDuringTeleport(Entity entity, CreatureSpawnEvent.SpawnReason reason) {
        addFreshEntity(entity, reason);
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

    @Inject(method = "explode", cancellable = true, at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/level/Explosion;interactsWithBlocks()Z"), locals = LocalCapture.CAPTURE_FAILHARD)
    private void arclight$doExplosion(Entity p_256039_, DamageSource p_255778_, ExplosionDamageCalculator p_256002_, double p_256067_, double p_256370_, double p_256153_, float p_256045_, boolean p_255686_, Level.ExplosionInteraction p_255827_, ParticleOptions p_310962_, ParticleOptions p_310322_, SoundEvent p_309795_,
                                      CallbackInfoReturnable<Explosion> cir, Explosion explosion) {
        if (((ExplosionBridge) explosion).bridge$wasCancelled()) {
            cir.setReturnValue(explosion);
        }
    }

    @Inject(method = "getMapData", at = @At("RETURN"))
    private void arclight$mapSetId(String id, CallbackInfoReturnable<MapItemSavedData> cir) {
        var data = cir.getReturnValue();
        if (data != null) {
            ((MapDataBridge) data).bridge$setId(id);
        }
    }

    @Inject(method = "setMapData", at = @At("HEAD"))
    private void arclight$mapSetId(String id, MapItemSavedData data, CallbackInfo ci) {
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

    /**
     * @author IzzelAliz
     * @reason
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Overwrite
    public static void makeObsidianPlatform(ServerLevel world) {
        BlockPos blockpos = END_SPAWN_POINT;
        int i = blockpos.getX();
        int j = blockpos.getY() - 2;
        int k = blockpos.getZ();
        BlockStateListPopulator blockList = new BlockStateListPopulator(world);
        BlockPos.betweenClosed(i - 2, j + 1, k - 2, i + 2, j + 3, k + 2).forEach((pos) -> {
            blockList.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        });
        BlockPos.betweenClosed(i - 2, j, k - 2, i + 2, j, k + 2).forEach((pos) -> {
            blockList.setBlock(pos, Blocks.OBSIDIAN.defaultBlockState(), 3);
        });
        if (!DistValidate.isValid(world)) {
            blockList.updateList();
            ArclightCaptures.getEndPortalEntity();
            return;
        }
        CraftWorld bworld = ((WorldBridge) world).bridge$getWorld();
        boolean spawnPortal = ArclightCaptures.getEndPortalSpawn();
        Entity entity = ArclightCaptures.getEndPortalEntity();
        PortalCreateEvent portalEvent = new PortalCreateEvent((List) blockList.getList(), bworld, entity == null ? null : ((EntityBridge) entity).bridge$getBukkitEntity(), PortalCreateEvent.CreateReason.END_PLATFORM);
        portalEvent.setCancelled(!spawnPortal);
        Bukkit.getPluginManager().callEvent(portalEvent);
        if (!portalEvent.isCancelled()) {
            blockList.updateList();
        }
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
