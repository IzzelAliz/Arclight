package io.izzel.arclight.common.mixin.core.world;

import io.izzel.arclight.common.bridge.world.WorldBridge;
import io.izzel.arclight.common.bridge.world.border.WorldBorderBridge;
import io.izzel.arclight.common.mod.ArclightMod;
import io.izzel.arclight.common.mod.server.ArclightServer;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.profiler.IProfiler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.DimensionType;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.IServerWorldInfo;
import net.minecraft.world.storage.ISpawnWorldInfo;
import net.minecraft.world.storage.IWorldInfo;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.CraftServer;
import org.bukkit.craftbukkit.v.CraftWorld;
import org.bukkit.craftbukkit.v.block.CraftBlock;
import org.bukkit.craftbukkit.v.block.data.CraftBlockData;
import org.bukkit.craftbukkit.v.event.CraftEventFactory;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.generator.ChunkGenerator;
import org.spigotmc.SpigotWorldConfig;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import javax.annotation.Nullable;
import java.util.function.Supplier;

@Mixin(World.class)
public abstract class WorldMixin implements WorldBridge {

    // @formatter:off
    @Shadow @Nullable public TileEntity getTileEntity(BlockPos pos) { return null; }
    @Shadow public abstract BlockState getBlockState(BlockPos pos);
    @Shadow public abstract void setTileEntity(BlockPos pos, @Nullable TileEntity tileEntityIn);
    @Shadow public abstract WorldBorder getWorldBorder();
    @Shadow @Final private WorldBorder worldBorder;
    @Shadow public abstract long getDayTime();
    @Shadow public abstract MinecraftServer shadow$getServer();
    @Shadow @Final private DimensionType dimensionType;
    @Shadow public abstract IWorldInfo getWorldInfo();
    @Shadow public abstract RegistryKey<World> getDimensionKey();
    @Accessor("mainThread") public abstract Thread arclight$getMainThread();
    // @formatter:on

    private RegistryKey<DimensionType> typeKey;
    protected CraftWorld world;
    public boolean pvpMode;
    public boolean keepSpawnInMemory = true;
    public long ticksPerAnimalSpawns;
    public long ticksPerMonsterSpawns;
    public long ticksPerWaterSpawns;
    public long ticksPerWaterAmbientSpawns;
    public long ticksPerAmbientSpawns;
    public boolean populating;
    public org.bukkit.generator.ChunkGenerator generator;
    protected org.bukkit.World.Environment environment;
    public org.spigotmc.SpigotWorldConfig spigotConfig;

    public void arclight$constructor(ISpawnWorldInfo worldInfo, RegistryKey<World> dimension, final DimensionType dimensionType, Supplier<IProfiler> profiler, boolean isRemote, boolean isDebug, long seed) {
        throw new RuntimeException();
    }

    public void arclight$constructor(ISpawnWorldInfo worldInfo, RegistryKey<World> dimension, final DimensionType dimensionType, Supplier<IProfiler> profiler, boolean isRemote, boolean isDebug, long seed, org.bukkit.generator.ChunkGenerator gen, org.bukkit.World.Environment env) {
        arclight$constructor(worldInfo, dimension, dimensionType, profiler, isRemote, isDebug, seed);
        this.generator = gen;
        this.environment = env;
        bridge$getWorld();
    }

    @Inject(method = "<init>(Lnet/minecraft/world/storage/ISpawnWorldInfo;Lnet/minecraft/util/RegistryKey;Lnet/minecraft/world/DimensionType;Ljava/util/function/Supplier;ZZJ)V", at = @At("RETURN"))
    private void arclight$init(ISpawnWorldInfo info, RegistryKey<World> dimension, DimensionType dimType, Supplier<IProfiler> profiler, boolean isRemote, boolean isDebug, long seed, CallbackInfo ci) {
        this.spigotConfig = new SpigotWorldConfig(((IServerWorldInfo) info).getWorldName());
        ((WorldBorderBridge) this.worldBorder).bridge$setWorld((World) (Object) this);
        this.ticksPerAnimalSpawns = this.getServer().getTicksPerAnimalSpawns();
        this.ticksPerMonsterSpawns = this.getServer().getTicksPerMonsterSpawns();
        this.ticksPerWaterSpawns = this.getServer().getTicksPerWaterSpawns();
        this.ticksPerWaterAmbientSpawns = this.getServer().getTicksPerWaterAmbientSpawns();
        this.ticksPerAmbientSpawns = this.getServer().getTicksPerAmbientSpawns();
        this.typeKey = this.getServer().getHandle().getServer().func_244267_aX().func_230520_a_().getOptionalKey(dimensionType)
            .orElseGet(() -> {
                Registry<DimensionType> registry = this.getServer().getHandle().getServer().func_244267_aX().func_230520_a_();
                RegistryKey<DimensionType> typeRegistryKey = RegistryKey.getOrCreateKey(registry.getRegistryKey(), dimension.getLocation());
                ArclightMod.LOGGER.warn("Assign {} to unknown dimension type {} as {}", typeRegistryKey, dimType);
                return typeRegistryKey;
            });
    }

    @Override
    public long bridge$ticksPerAnimalSpawns() {
        return ticksPerAnimalSpawns;
    }

    @Override
    public long bridge$ticksPerMonsterSpawns() {
        return ticksPerMonsterSpawns;
    }

    @Override
    public long bridge$ticksPerWaterSpawns() {
        return ticksPerWaterSpawns;
    }

    @Override
    public long bridge$ticksPerAmbientSpawns() {
        return ticksPerAmbientSpawns;
    }

    @Override
    public long bridge$ticksPerWaterAmbientSpawns() {
        return ticksPerWaterAmbientSpawns;
    }

    public RegistryKey<DimensionType> getTypeKey() {
        return this.typeKey;
    }

    @Override
    public RegistryKey<DimensionType> bridge$getTypeKey() {
        return getTypeKey();
    }

    @Override
    public SpigotWorldConfig bridge$spigotConfig() {
        return this.spigotConfig;
    }

    @Inject(method = "setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;I)Z",
        at = @At("HEAD"), cancellable = true)
    private void arclight$hooks(BlockPos pos, BlockState newState, int flags, CallbackInfoReturnable<Boolean> cir) {
        if (!processCaptures(pos, newState, flags)) {
            cir.setReturnValue(false);
        }
    }

    private boolean processCaptures(BlockPos pos, BlockState newState, int flags) {
        Entity entityChangeBlock = ArclightCaptures.getEntityChangeBlock();
        if (entityChangeBlock != null) {
            if (CraftEventFactory.callEntityChangeBlockEvent(entityChangeBlock, pos, newState).isCancelled()) {
                return false;
            }
        }
        return true;
    }

    @Inject(method = "markAndNotifyBlock", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/block/BlockState;updateNeighbours(Lnet/minecraft/world/IWorld;Lnet/minecraft/util/math/BlockPos;II)V"))
    private void arclight$callBlockPhysics(BlockPos pos, Chunk chunk, BlockState blockstate, BlockState state, int flags, int recursionLeft, CallbackInfo ci) {
        if (this.world != null) {
            BlockPhysicsEvent event = new BlockPhysicsEvent(CraftBlock.at((IWorld) this, pos), CraftBlockData.fromData(state));
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "neighborChanged", cancellable = true,
        at = @At(value = "INVOKE", target = "Lnet/minecraft/block/BlockState;neighborChanged(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/Block;Lnet/minecraft/util/math/BlockPos;Z)V"),
        locals = LocalCapture.CAPTURE_FAILHARD)
    private void arclight$callBlockPhysics2(BlockPos pos, Block blockIn, BlockPos fromPos, CallbackInfo ci, BlockState blockState) {
        if (this.world != null) {
            IWorld iWorld = (IWorld) this;
            BlockPhysicsEvent event = new BlockPhysicsEvent(CraftBlock.at(iWorld, pos), CraftBlockData.fromData(blockState), CraftBlock.at(iWorld, fromPos));
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                ci.cancel();
            }
        }
    }

    public CraftServer getServer() {
        return (CraftServer) Bukkit.getServer();
    }

    public CraftWorld getWorld() {
        if (this.world == null) {
            if (generator == null) {
                generator = getServer().getGenerator(((IServerWorldInfo) this.getWorldInfo()).getWorldName());
            }
            if (environment == null) {
                environment = ArclightServer.getEnvironment(this.typeKey);
            }
            this.world = new CraftWorld((ServerWorld) (Object) this, generator, environment);
            getServer().addWorld(this.world);
        }
        return this.world;
    }

    public TileEntity getTileEntity(BlockPos pos, boolean validate) {
        return getTileEntity(pos);
    }

    @Override
    public TileEntity bridge$getTileEntity(BlockPos pos, boolean validate) {
        return getTileEntity(pos, validate);
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
}
