package io.izzel.arclight.mixin.core.world.server;

import com.google.common.collect.Lists;
import io.izzel.arclight.bridge.entity.EntityBridge;
import io.izzel.arclight.bridge.inventory.IInventoryBridge;
import io.izzel.arclight.bridge.world.ExplosionBridge;
import io.izzel.arclight.bridge.world.server.ServerWorldBridge;
import io.izzel.arclight.bridge.world.storage.WorldInfoBridge;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.LightningBoltEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.IInventory;
import net.minecraft.network.IPacket;
import net.minecraft.particles.IParticleData;
import net.minecraft.profiler.IProfiler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.IProgressUpdate;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Explosion;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.listener.IChunkStatusListener;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.MapData;
import net.minecraft.world.storage.SaveHandler;
import net.minecraft.world.storage.WorldInfo;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_14_R1.entity.CraftHumanEntity;
import org.bukkit.craftbukkit.v1_14_R1.event.CraftEventFactory;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.LightningStrike;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.server.MapInitializeEvent;
import org.bukkit.event.weather.LightningStrikeEvent;
import org.bukkit.event.world.WorldSaveEvent;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import io.izzel.arclight.bridge.world.storage.MapDataBridge;
import io.izzel.arclight.mixin.core.world.WorldMixin;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Random;
import java.util.concurrent.Executor;
import java.util.logging.Level;

@Mixin(ServerWorld.class)
@Implements(@Interface(iface = ServerWorldBridge.Hack.class, prefix = "hack$"))
public abstract class ServerWorldMixin extends WorldMixin implements ServerWorldBridge {

    // @formatter:off
    @Shadow public abstract boolean addEntity(Entity entityIn);
    @Shadow public abstract boolean summonEntity(Entity entityIn);
    @Shadow public abstract void addLightningBolt(LightningBoltEntity entityIn);
    @Shadow public abstract <T extends IParticleData> int spawnParticle(T type, double posX, double posY, double posZ, int particleCount, double xOffset, double yOffset, double zOffset, double speed);
    @Shadow protected abstract boolean sendPacketWithinDistance(ServerPlayerEntity player, boolean longDistance, double posX, double posY, double posZ, IPacket<?> packet);
    @Shadow @Nonnull public abstract MinecraftServer shadow$getServer();
    // @formatter:on

    public void arclight$constructor(MinecraftServer serverIn, Executor executor, SaveHandler saveHandler, WorldInfo worldInfo, DimensionType dimType, IProfiler profiler, IChunkStatusListener listener) {
        throw new RuntimeException();
    }

    public void arclight$constructor(MinecraftServer serverIn, Executor executor, SaveHandler saveHandler, WorldInfo worldInfo, DimensionType dimType, IProfiler profiler, IChunkStatusListener listener, org.bukkit.generator.ChunkGenerator gen, org.bukkit.World.Environment env) {
        arclight$constructor(serverIn, executor, saveHandler, worldInfo, dimType, profiler, listener);
        this.generator = gen;
        this.environment = env;
        bridge$getWorld();
    }

    @Inject(method = "onEntityAdded", at = @At("RETURN"))
    private void arclight$validEntity(Entity entityIn, CallbackInfo ci) {
        ((EntityBridge) entityIn).bridge$setValid(true);
    }

    @Inject(method = "removeEntityComplete", remap = false, at = @At("RETURN"))
    private void arclight$invalidEntity(Entity entityIn, boolean keepData, CallbackInfo ci) {
        ((EntityBridge) entityIn).bridge$setValid(false);
    }

    @Inject(method = "tickEnvironment", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/server/ServerWorld;addEntity(Lnet/minecraft/entity/Entity;)Z"))
    public void arclight$thunder(Chunk chunkIn, int randomTickSpeed, CallbackInfo ci) {
        bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.LIGHTNING);
    }

    @Redirect(method = "tickEnvironment", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/server/ServerWorld;setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)Z"))
    public boolean arclight$snowForm(ServerWorld serverWorld, BlockPos pos, BlockState state) {
        return CraftEventFactory.handleBlockFormEvent(serverWorld, pos, state, null);
    }

    @Inject(method = "createSpawnPosition", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/biome/provider/BiomeProvider;getBiomesToSpawnIn()Ljava/util/List;"))
    public void arclight$spawnPoint(WorldSettings settings, CallbackInfo ci) {
        if (this.generator != null) {
            Random rand = new Random(this.getSeed());
            org.bukkit.Location spawn = this.generator.getFixedSpawnLocation(bridge$getWorld(), rand);

            if (spawn != null) {
                if (spawn.getWorld() != (bridge$getWorld())) {
                    throw new IllegalStateException("Cannot set spawn point for " + this.worldInfo.getWorldName() + " to be in another world (" + spawn.getWorld().getName() + ")");
                } else {
                    this.worldInfo.setSpawn(new BlockPos(spawn.getBlockX(), spawn.getBlockY(), spawn.getBlockZ()));
                    ci.cancel();
                }
            }
        }
    }

    @Inject(method = "save", at = @At(value = "JUMP", ordinal = 0, opcode = Opcodes.IFNULL))
    public void arclight$worldSave(IProgressUpdate progress, boolean flush, boolean skipSave, CallbackInfo ci) {
        Bukkit.getPluginManager().callEvent(new WorldSaveEvent(bridge$getWorld()));
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

    @Inject(method = "removeEntityComplete", remap = false, at = @At("RETURN"))
    public void arclight$invalidateEntity(Entity entityIn, boolean keepData, CallbackInfo ci) {
        ((EntityBridge) entityIn).bridge$setValid(false);
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    @Nullable
    public MapData getMapData(String mapName) {
        return this.shadow$getServer().func_71218_a(DimensionType.OVERWORLD).getSavedData().get(() -> {
            MapData newMap = new MapData(mapName);
            MapInitializeEvent event = new MapInitializeEvent(((MapDataBridge) newMap).bridge$getMapView());
            Bukkit.getServer().getPluginManager().callEvent(event);
            return newMap;
        }, mapName);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    public void arclight$init(MinecraftServer serverIn, Executor executor, SaveHandler saveHandler, WorldInfo worldInfo, DimensionType dimType, IProfiler profiler, IChunkStatusListener listener, CallbackInfo ci) {
        ((WorldInfoBridge) worldInfo).bridge$setWorld((ServerWorld) (Object) this);
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

    @Inject(method = "addLightningBolt", cancellable = true, at = @At("HEAD"))
    public void arclight$lightningEvent(LightningBoltEntity entityIn, CallbackInfo ci) {
        LightningStrikeEvent event = new LightningStrikeEvent(this.getWorld(), (LightningStrike) ((EntityBridge) entityIn).bridge$getBukkitEntity(), arclight$cause);
        Bukkit.getPluginManager().callEvent(event);

        if (event.isCancelled()) {
            ci.cancel();
        }
        arclight$cause = null;
    }

    @Override
    public void bridge$pushStrikeLightningCause(LightningStrikeEvent.Cause cause) {
        this.arclight$cause = cause;
    }

    public void strikeLightning(LightningBoltEntity entity, LightningStrikeEvent.Cause cause) {
        bridge$pushStrikeLightningCause(cause);
        this.addLightningBolt(entity);
    }

    @Override
    public void bridge$strikeLightning(LightningBoltEntity entity, LightningStrikeEvent.Cause cause) {
        strikeLightning(entity, cause);
    }

    private transient CreatureSpawnEvent.SpawnReason arclight$reason;

    @Inject(method = "addEntity0", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraftforge/eventbus/api/IEventBus;post(Lnet/minecraftforge/eventbus/api/Event;)Z"))
    public void arclight$addEntityEvent(Entity entityIn, CallbackInfoReturnable<Boolean> cir) {
        if (arclight$reason == null) arclight$reason = CreatureSpawnEvent.SpawnReason.DEFAULT;
        if (!CraftEventFactory.doEntityAddEventCalling((ServerWorld) (Object) this, entityIn, arclight$reason)) {
            cir.setReturnValue(false);
        }
        arclight$reason = null;
    }

    @Inject(method = "addEntity0", at = @At("RETURN"))
    public void arclight$resetReason(Entity entityIn, CallbackInfoReturnable<Boolean> cir) {
        arclight$reason = null;
    }

    @Override
    public void bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason reason) {
        this.arclight$reason = reason;
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

    @Inject(method = "createExplosion", cancellable = true, at = @At(value = "INVOKE", shift = At.Shift.AFTER,
        target = "Lnet/minecraft/world/Explosion;doExplosionB(Z)V"), locals = LocalCapture.CAPTURE_FAILHARD)
    public void arclight$doExplosion(Entity entityIn, DamageSource damageSourceIn, double xIn, double yIn, double zIn,
                                     float explosionRadius, boolean causesFire, Explosion.Mode modeIn, CallbackInfoReturnable<Explosion> cir,
                                     Explosion explosion) {
        if (((ExplosionBridge) explosion).bridge$wasCancelled()) {
            cir.setReturnValue(explosion);
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
        TileEntity result = super.getTileEntity(pos);
        if (Thread.currentThread() != arclight$getMainThread()) {
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
        this.getServer().getLogger().log(Level.SEVERE, "Block at {0}, {1}, {2} is {3} but has {4}" + ". "
            + "Bukkit will attempt to fix this, but there may be additional damage that we cannot recover.", new Object[]{pos.getX(), pos.getY(), pos.getZ(), type, found});

        if (type.hasTileEntity(state)) {
            TileEntity replacement = type.createTileEntity(state, (IBlockReader) this);
            if (replacement == null) return found;
            replacement.setWorld((World) (Object) this);
            this.setTileEntity(pos, replacement);
            return replacement;
        } else {
            return found;
        }
    }
}
