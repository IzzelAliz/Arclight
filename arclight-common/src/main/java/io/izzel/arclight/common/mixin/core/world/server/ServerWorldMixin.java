package io.izzel.arclight.common.mixin.core.world.server;

import com.google.common.collect.Lists;
import io.izzel.arclight.common.bridge.entity.EntityBridge;
import io.izzel.arclight.common.bridge.entity.player.ServerPlayerEntityBridge;
import io.izzel.arclight.common.bridge.inventory.IInventoryBridge;
import io.izzel.arclight.common.bridge.world.ExplosionBridge;
import io.izzel.arclight.common.bridge.world.WorldBridge;
import io.izzel.arclight.common.bridge.world.server.ServerChunkProviderBridge;
import io.izzel.arclight.common.bridge.world.server.ServerWorldBridge;
import io.izzel.arclight.common.bridge.world.storage.DerivedWorldInfoBridge;
import io.izzel.arclight.common.bridge.world.storage.MapDataBridge;
import io.izzel.arclight.common.bridge.world.storage.WorldInfoBridge;
import io.izzel.arclight.common.mixin.core.world.WorldMixin;
import io.izzel.arclight.common.mod.server.world.WorldSymlink;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import io.izzel.arclight.common.mod.util.DelegateWorldInfo;
import io.izzel.arclight.i18n.ArclightConfig;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.LightningBoltEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.IInventory;
import net.minecraft.network.IPacket;
import net.minecraft.network.play.server.SSpawnParticlePacket;
import net.minecraft.particles.IParticleData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.IProgressUpdate;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.DimensionType;
import net.minecraft.world.Explosion;
import net.minecraft.world.ExplosionContext;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.NextTickListEntry;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.listener.IChunkStatusListener;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.server.ServerChunkProvider;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.spawner.ISpecialSpawner;
import net.minecraft.world.storage.DerivedWorldInfo;
import net.minecraft.world.storage.IServerWorldInfo;
import net.minecraft.world.storage.MapData;
import net.minecraft.world.storage.SaveFormat;
import net.minecraft.world.storage.ServerWorldInfo;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.CraftWorld;
import org.bukkit.craftbukkit.v.entity.CraftHumanEntity;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.craftbukkit.v.generator.CustomChunkGenerator;
import org.bukkit.craftbukkit.v.util.BlockStateListPopulator;
import org.bukkit.craftbukkit.v.util.WorldUUID;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.LightningStrike;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.server.MapInitializeEvent;
import org.bukkit.event.weather.LightningStrikeEvent;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.event.world.TimeSkipEvent;
import org.bukkit.event.world.WorldSaveEvent;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
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
import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.logging.Level;

@Mixin(ServerWorld.class)
@Implements(@Interface(iface = ServerWorldBridge.Hack.class, prefix = "hack$"))
public abstract class ServerWorldMixin extends WorldMixin implements ServerWorldBridge {

    // @formatter:off
    @Shadow public abstract boolean addEntity(Entity entityIn);
    @Shadow public abstract boolean summonEntity(Entity entityIn);
    @Shadow public abstract <T extends IParticleData> int spawnParticle(T type, double posX, double posY, double posZ, int particleCount, double xOffset, double yOffset, double zOffset, double speed);
    @Shadow protected abstract boolean sendPacketWithinDistance(ServerPlayerEntity player, boolean longDistance, double posX, double posY, double posZ, IPacket<?> packet);
    @Shadow @Nonnull public abstract MinecraftServer shadow$getServer();
    @Shadow @Final private List<ServerPlayerEntity> players;
    @Shadow @Final public Int2ObjectMap<Entity> entitiesById;
    @Shadow public abstract ServerChunkProvider getChunkProvider();
    @Shadow private boolean allPlayersSleeping;
    @Shadow protected abstract void wakeUpAllPlayers();
    @Shadow @Final private ServerChunkProvider serverChunkProvider;
    @Shadow protected abstract boolean hasDuplicateEntity(Entity entityIn);
    @Shadow @Final public static BlockPos END_SPAWN_AREA;
    @Shadow @Final public IServerWorldInfo serverWorldInfo;
    // @formatter:on

    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    public ServerWorldInfo $$worldDataServer;
    public SaveFormat.LevelSave convertable;
    public UUID uuid;

    public void arclight$constructor(MinecraftServer server, Executor backgroundExecutor, SaveFormat.LevelSave levelSave, IServerWorldInfo serverWorldInfo, RegistryKey<World> dimension, DimensionType dimensionType, IChunkStatusListener statusListener, ChunkGenerator chunkGenerator, boolean isDebug, long seed, List<ISpecialSpawner> specialSpawners, boolean shouldBeTicking) {
        throw new RuntimeException();
    }

    public void arclight$constructor(MinecraftServer server, Executor backgroundExecutor, SaveFormat.LevelSave levelSave, IServerWorldInfo serverWorldInfo, RegistryKey<World> dimension, DimensionType dimensionType, IChunkStatusListener statusListener, ChunkGenerator chunkGenerator, boolean isDebug, long seed, List<ISpecialSpawner> specialSpawners, boolean shouldBeTicking, org.bukkit.World.Environment env, org.bukkit.generator.ChunkGenerator gen) {
        arclight$constructor(server, backgroundExecutor, levelSave, serverWorldInfo, dimension, dimensionType, statusListener, chunkGenerator, isDebug, seed, specialSpawners, shouldBeTicking);
        this.generator = gen;
        this.environment = env;
        if (gen != null) {
            CustomChunkGenerator generator = new CustomChunkGenerator((ServerWorld) (Object) this, this.serverChunkProvider.getChunkGenerator(), gen);
            ((ServerChunkProviderBridge) this.serverChunkProvider).bridge$setChunkGenerator(generator);
        }
        bridge$getWorld();
    }

    @Inject(method = "<init>(Lnet/minecraft/server/MinecraftServer;Ljava/util/concurrent/Executor;Lnet/minecraft/world/storage/SaveFormat$LevelSave;Lnet/minecraft/world/storage/IServerWorldInfo;Lnet/minecraft/util/RegistryKey;Lnet/minecraft/world/DimensionType;Lnet/minecraft/world/chunk/listener/IChunkStatusListener;Lnet/minecraft/world/gen/ChunkGenerator;ZJLjava/util/List;Z)V", at = @At("RETURN"))
    private void arclight$init(MinecraftServer minecraftServer, Executor backgroundExecutor, SaveFormat.LevelSave levelSave, IServerWorldInfo worldInfo, RegistryKey<World> dimension, DimensionType dimensionType, IChunkStatusListener statusListener, ChunkGenerator chunkGenerator, boolean isDebug, long seed, List<ISpecialSpawner> specialSpawners, boolean shouldBeTicking, CallbackInfo ci) {
        this.pvpMode = minecraftServer.isPVPEnabled();
        this.convertable = levelSave;
        this.uuid = WorldUUID.getUUID(levelSave.getDimensionFolder(this.getDimensionKey()));
        if (worldInfo instanceof ServerWorldInfo) {
            this.$$worldDataServer = (ServerWorldInfo) worldInfo;
        } else if (worldInfo instanceof DerivedWorldInfo) {
            // damn spigot again
            this.$$worldDataServer = DelegateWorldInfo.wrap(((DerivedWorldInfo) worldInfo));
            ((DerivedWorldInfoBridge) worldInfo).bridge$setDimType(this.getTypeKey());
            if (ArclightConfig.spec().getCompat().isSymlinkWorld()) {
                WorldSymlink.create((DerivedWorldInfo) worldInfo, levelSave.getDimensionFolder(this.getDimensionKey()));
            }
        }
        ((ServerChunkProviderBridge) this.serverChunkProvider).bridge$setViewDistance(spigotConfig.viewDistance);
        ((WorldInfoBridge) this.$$worldDataServer).bridge$setWorld((ServerWorld) (Object) this);
    }

    public Chunk getChunkIfLoaded(int x, int z) {
        return this.serverChunkProvider.getChunk(x, z, false);
    }

    public <T extends IParticleData> int sendParticles(final ServerPlayerEntity sender, final T t0, final double d0, final double d1, final double d2, final int i, final double d3, final double d4, final double d5, final double d6, final boolean force) {
        SSpawnParticlePacket packet = new SSpawnParticlePacket(t0, force, d0, d1, d2, (float) d3, (float) d4, (float) d5, (float) d6, i);
        int j = 0;
        for (ServerPlayerEntity entity : this.players) {
            if (sender == null || ((ServerPlayerEntityBridge) entity).bridge$getBukkitEntity().canSee(((ServerPlayerEntityBridge) sender).bridge$getBukkitEntity())) {
                if (this.sendPacketWithinDistance(entity, force, d0, d1, d2, packet)) {
                    ++j;
                }
            }
        }
        return j;
    }

    @Override
    public SaveFormat.LevelSave bridge$getConvertable() {
        return this.convertable;
    }

    @Inject(method = "onEntityAdded", at = @At("RETURN"))
    private void arclight$validEntity(Entity entityIn, CallbackInfo ci) {
        ((EntityBridge) entityIn).bridge$setValid(true);
    }

    @Inject(method = "updateEntity", at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/entity/Entity;tick()V"))
    private void arclight$tickPortal(Entity entityIn, CallbackInfo ci) {
        ((EntityBridge) entityIn).bridge$postTick();
    }

    @Inject(method = "tickPassenger", at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/entity/Entity;updateRidden()V"))
    private void arclight$tickPortalPassenger(Entity ridingEntity, Entity passengerEntity, CallbackInfo ci) {
        ((EntityBridge) passengerEntity).bridge$postTick();
    }

    @Inject(method = "removeEntityComplete", remap = false, at = @At("RETURN"))
    private void arclight$invalidEntity(Entity entityIn, boolean keepData, CallbackInfo ci) {
        ((EntityBridge) entityIn).bridge$setValid(false);
    }

    @Inject(method = "tickEnvironment", at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/world/server/ServerWorld;addEntity(Lnet/minecraft/entity/Entity;)Z"))
    public void arclight$thunder(Chunk chunkIn, int randomTickSpeed, CallbackInfo ci) {
        bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.LIGHTNING);
    }

    @Redirect(method = "tickEnvironment", at = @At(value = "INVOKE", ordinal = 1, target = "Lnet/minecraft/world/server/ServerWorld;addEntity(Lnet/minecraft/entity/Entity;)Z"))
    private boolean arclight$thunder(ServerWorld serverWorld, Entity entityIn) {
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
        LightningStrikeEvent lightning = CraftEventFactory.callLightningStrikeEvent((LightningStrike) ((EntityBridge) entity).bridge$getBukkitEntity(), cause);
        if (lightning.isCancelled()) {
            return false;
        }
        return this.addEntity(entity);
    }

    @Redirect(method = "tickEnvironment", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/server/ServerWorld;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)Z"))
    public boolean arclight$snowForm(ServerWorld serverWorld, BlockPos pos, BlockState state) {
        return CraftEventFactory.handleBlockFormEvent(serverWorld, pos, state, null);
    }

    @Inject(method = "save", at = @At(value = "JUMP", ordinal = 0, opcode = Opcodes.IFNULL))
    private void arclight$worldSaveEvent(IProgressUpdate progress, boolean flush, boolean skipSave, CallbackInfo ci) {
        Bukkit.getPluginManager().callEvent(new WorldSaveEvent(bridge$getWorld()));
    }

    @Inject(method = "save", at = @At("RETURN"))
    private void arclight$saveLevelDat(IProgressUpdate progress, boolean flush, boolean skipSave, CallbackInfo ci) {
        if (this.serverWorldInfo instanceof ServerWorldInfo) {
            ServerWorldInfo worldInfo = (ServerWorldInfo) this.serverWorldInfo;
            worldInfo.setWorldBorderSerializer(this.getWorldBorder().getSerializer());
            worldInfo.setCustomBossEventData(this.shadow$getServer().getCustomBossEvents().write());
            this.convertable.saveLevel(this.shadow$getServer().dynamicRegistries, worldInfo, this.shadow$getServer().getPlayerList().getHostPlayerData());
        }
    }

    @Inject(method = "onChunkUnloading", at = @At("HEAD"))
    public void arclight$closeOnChunkUnloading(Chunk chunkIn, CallbackInfo ci) {
        for (TileEntity tileentity : chunkIn.getTileEntityMap().values()) {
            if (tileentity instanceof IInventory) {
                for (HumanEntity h : Lists.newArrayList(((IInventoryBridge) tileentity).getViewers())) {
                    if (h instanceof CraftHumanEntity) {
                        ((CraftHumanEntity) h).getHandle().closeScreen();
                    }
                }
            }
        }
    }

    private transient boolean arclight$force;

    @Redirect(method = "spawnParticle(Lnet/minecraft/particles/IParticleData;DDDIDDDD)I", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/server/ServerWorld;sendPacketWithinDistance(Lnet/minecraft/entity/player/ServerPlayerEntity;ZDDDLnet/minecraft/network/IPacket;)Z"))
    public boolean arclight$particleVisible(ServerWorld serverWorld, ServerPlayerEntity player, boolean longDistance, double posX, double posY, double posZ, IPacket<?> packet) {
        return this.sendPacketWithinDistance(player, arclight$force, posX, posY, posZ, packet);
    }

    public <T extends IParticleData> int sendParticles(T type, double posX, double posY, double posZ, int particleCount, double xOffset, double yOffset, double zOffset, double speed, boolean force) {
        arclight$force = force;
        return this.spawnParticle(type, posX, posY, posZ, particleCount, xOffset, yOffset, zOffset, speed);
    }

    @Override
    public <T extends IParticleData> int bridge$sendParticles(T type, double posX, double posY, double posZ, int particleCount, double xOffset, double yOffset, double zOffset, double speed, boolean force) {
        return this.sendParticles(type, posX, posY, posZ, particleCount, xOffset, yOffset, zOffset, speed, force);
    }

    private transient LightningStrikeEvent.Cause arclight$cause;

    @Override
    public void bridge$pushStrikeLightningCause(LightningStrikeEvent.Cause cause) {
        this.arclight$cause = cause;
    }

    @Override
    public void bridge$strikeLightning(LightningBoltEntity entity, LightningStrikeEvent.Cause cause) {
        strikeLightning(entity, cause);
    }

    private transient CreatureSpawnEvent.SpawnReason arclight$reason;

    @Inject(method = "addEntity0", cancellable = true, at = @At(value = "INVOKE", remap = false, target = "Lnet/minecraftforge/eventbus/api/IEventBus;post(Lnet/minecraftforge/eventbus/api/Event;)Z"))
    private void arclight$addEntityEvent(Entity entityIn, CallbackInfoReturnable<Boolean> cir) {
        CreatureSpawnEvent.SpawnReason reason = arclight$reason == null ? CreatureSpawnEvent.SpawnReason.DEFAULT : arclight$reason;
        arclight$reason = null;
        if (!CraftEventFactory.doEntityAddEventCalling((ServerWorld) (Object) this, entityIn, reason)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "addEntity0", at = @At("RETURN"))
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

    public boolean addEntity(Entity entity, CreatureSpawnEvent.SpawnReason reason) {
        bridge$pushAddEntityReason(reason);
        return addEntity(entity);
    }

    @Override
    public boolean bridge$addEntity(Entity entity, CreatureSpawnEvent.SpawnReason reason) {
        return addEntity(entity, reason);
    }

    public boolean addEntitySerialized(Entity entity, CreatureSpawnEvent.SpawnReason reason) {
        bridge$pushAddEntityReason(reason);
        return summonEntity(entity);
    }

    @Override
    public boolean bridge$addEntitySerialized(Entity entity, CreatureSpawnEvent.SpawnReason reason) {
        return addEntitySerialized(entity, reason);
    }

    public boolean addAllEntitiesSafely(Entity entity, CreatureSpawnEvent.SpawnReason reason) {
        if (entity.getSelfAndPassengers().anyMatch(this::hasDuplicateEntity)) {
            return false;
        }
        return this.bridge$addAllEntities(entity, reason);
    }

    @Override
    public boolean bridge$addAllEntitiesSafely(Entity entity, CreatureSpawnEvent.SpawnReason reason) {
        return addAllEntitiesSafely(entity, reason);
    }

    @Inject(method = "createExplosion", cancellable = true, at = @At(value = "INVOKE", shift = At.Shift.AFTER,
        target = "Lnet/minecraft/world/Explosion;doExplosionB(Z)V"), locals = LocalCapture.CAPTURE_FAILHARD)
    public void arclight$doExplosion(Entity entityIn, DamageSource damageSourceIn, @Nullable ExplosionContext context, double xIn, double yIn, double zIn,
                                     float explosionRadius, boolean causesFire, Explosion.Mode modeIn, CallbackInfoReturnable<Explosion> cir,
                                     Explosion explosion) {
        if (((ExplosionBridge) explosion).bridge$wasCancelled()) {
            cir.setReturnValue(explosion);
        }
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    @Nullable
    public MapData getMapData(String mapName) {
        return this.shadow$getServer().func_241755_D_().getSavedData().get(() -> {
            MapData newMap = new MapData(mapName);
            MapInitializeEvent event = new MapInitializeEvent(((MapDataBridge) newMap).bridge$getMapView());
            Bukkit.getServer().getPluginManager().callEvent(event);
            return newMap;
        }, mapName);
    }

    @Inject(method = "updateBlock", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/server/ServerWorld;notifyNeighborsOfStateChange(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/Block;)V"))
    private void arclight$returnIfPopulate(BlockPos pos, Block block, CallbackInfo ci) {
        if (populating) {
            ci.cancel();
        }
    }

    @Inject(method = "notifyBlockUpdate", cancellable = true, at = @At("HEAD"))
    private void arclight$skipOutboundUpdate(BlockPos pos, BlockState oldState, BlockState newState, int flags, CallbackInfo ci) {
        if (World.isOutsideBuildHeight(pos)) {
            ci.cancel();
        }
    }

    @Override
    public TileEntity bridge$getTileEntity(BlockPos blockPos) {
        return this.getTileEntity(blockPos);
    }

    public TileEntity hack$getTileEntity(BlockPos pos) {
        return this.getTileEntity(pos);
    }

    @Override
    public TileEntity getTileEntity(BlockPos pos) {
        return getTileEntity(pos, true);
    }

    @Override
    public TileEntity getTileEntity(BlockPos pos, boolean validate) {
        TileEntity result = super.getTileEntity(pos);
        if (!validate || Thread.currentThread() != arclight$getMainThread()) {
            return result;
        }

        BlockState state = getBlockState(pos);
        Block type = state.getBlock();

        if (result != null && type != Blocks.AIR) {
            if (!result.getType().isValidBlock(type)) {
                result = fixTileEntity(pos, state, type, result);
            }
        }

        return result;
    }

    private TileEntity fixTileEntity(BlockPos pos, Block type, TileEntity found) {
        return fixTileEntity(pos, getBlockState(pos), type, found);
    }

    private TileEntity fixTileEntity(BlockPos pos, BlockState state, Block type, TileEntity found) {
        ResourceLocation registryName = found.getType().getRegistryName();
        if (registryName == null || !registryName.getNamespace().equals("minecraft")) {
            return found;
        }
        ResourceLocation blockType = type.getRegistryName();
        if (blockType == null || !blockType.getNamespace().equals("minecraft")) {
            return found;
        }
        this.getServer().getLogger().log(Level.SEVERE, "Block at {0}, {1}, {2} is {3} but has {4}" + ". "
            + "Bukkit will attempt to fix this, but there may be additional damage that we cannot recover.", new Object[]{pos.getX(), pos.getY(), pos.getZ(), type, found});

        if (type.hasTileEntity(state)) {
            TileEntity replacement = type.createTileEntity(state, (IBlockReader) this);
            if (replacement == null) return found;
            replacement.setWorldAndPos(((World) (Object) this), pos);
            this.setTileEntity(pos, replacement);
            return replacement;
        } else {
            return found;
        }
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/server/ServerWorld;setDayTime(J)V"))
    private void arclight$timeSkip(ServerWorld world, long time) {
        TimeSkipEvent event = new TimeSkipEvent(this.bridge$getWorld(), TimeSkipEvent.SkipReason.NIGHT_SKIP, (time - time % 24000L) - this.getDayTime());
        Bukkit.getPluginManager().callEvent(event);
        arclight$timeSkipCancelled = event.isCancelled();
        if (!event.isCancelled()) {
            world.setDayTime(this.getDayTime() + event.getSkipAmount());
            this.allPlayersSleeping = this.players.stream().allMatch(LivingEntity::isSleeping);
        }
    }

    private transient boolean arclight$timeSkipCancelled;

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/server/ServerWorld;wakeUpAllPlayers()V"))
    private void arclight$notWakeIfCancelled(ServerWorld world) {
        if (!arclight$timeSkipCancelled) {
            this.wakeUpAllPlayers();
        }
        arclight$timeSkipCancelled = false;
    }

    @Override
    public ServerWorld bridge$getMinecraftWorld() {
        return (ServerWorld) (Object) this;
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Overwrite
    public static void setupEndSpawnPlatform(ServerWorld world) {
        BlockPos blockpos = END_SPAWN_AREA;
        int i = blockpos.getX();
        int j = blockpos.getY() - 2;
        int k = blockpos.getZ();
        BlockStateListPopulator blockList = new BlockStateListPopulator(world);
        BlockPos.getAllInBoxMutable(i - 2, j + 1, k - 2, i + 2, j + 3, k + 2).forEach((pos) -> {
            blockList.setBlockState(pos, Blocks.AIR.getDefaultState(), 3);
        });
        BlockPos.getAllInBoxMutable(i - 2, j, k - 2, i + 2, j, k + 2).forEach((pos) -> {
            blockList.setBlockState(pos, Blocks.OBSIDIAN.getDefaultState(), 3);
        });
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

    @ModifyVariable(method = "tickBlock", ordinal = 0, argsOnly = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/block/BlockState;tick(Lnet/minecraft/world/server/ServerWorld;Lnet/minecraft/util/math/BlockPos;Ljava/util/Random;)V"))
    private NextTickListEntry<Block> arclight$captureTickingBlock(NextTickListEntry<Block> blockTickEntry) {
        ArclightCaptures.captureTickingBlock((ServerWorld) (Object) this, blockTickEntry.position);
        return blockTickEntry;
    }

    @ModifyVariable(method = "tickBlock", ordinal = 0, argsOnly = true, at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/block/BlockState;tick(Lnet/minecraft/world/server/ServerWorld;Lnet/minecraft/util/math/BlockPos;Ljava/util/Random;)V"))
    private NextTickListEntry<Block> arclight$resetTickingBlock(NextTickListEntry<Block> blockTickEntry) {
        ArclightCaptures.resetTickingBlock();
        return blockTickEntry;
    }

    @ModifyVariable(method = "tickEnvironment", ordinal = 0, at = @At(value = "INVOKE", target = "Lnet/minecraft/block/BlockState;randomTick(Lnet/minecraft/world/server/ServerWorld;Lnet/minecraft/util/math/BlockPos;Ljava/util/Random;)V"))
    private BlockPos arclight$captureRandomTick(BlockPos pos) {
        ArclightCaptures.captureTickingBlock((ServerWorld) (Object) this, pos);
        return pos;
    }

    @ModifyVariable(method = "tickEnvironment", ordinal = 0, at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/block/BlockState;randomTick(Lnet/minecraft/world/server/ServerWorld;Lnet/minecraft/util/math/BlockPos;Ljava/util/Random;)V"))
    private BlockPos arclight$resetRandomTick(BlockPos pos) {
        ArclightCaptures.resetTickingBlock();
        return pos;
    }

    @ModifyVariable(method = "updateEntity", argsOnly = true, ordinal = 0, at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;tick()V"))
    private Entity arclight$captureTickingEntity(Entity entity) {
        ArclightCaptures.captureTickingEntity(entity);
        return entity;
    }

    @ModifyVariable(method = "updateEntity", argsOnly = true, ordinal = 0, at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/entity/Entity;tick()V"))
    private Entity arclight$resetTickingEntity(Entity entity) {
        ArclightCaptures.resetTickingEntity();
        return entity;
    }

    @ModifyVariable(method = "tickPassenger", argsOnly = true, ordinal = 1, at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;updateRidden()V"))
    private Entity arclight$captureTickingPassenger(Entity entity) {
        ArclightCaptures.captureTickingEntity(entity);
        return entity;
    }

    @ModifyVariable(method = "tickPassenger", argsOnly = true, ordinal = 1, at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/entity/Entity;updateRidden()V"))
    private Entity arclight$resetTickingPassenger(Entity entity) {
        ArclightCaptures.resetTickingEntity();
        return entity;
    }
}
