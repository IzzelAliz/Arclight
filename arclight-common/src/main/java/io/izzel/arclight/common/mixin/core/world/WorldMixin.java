package io.izzel.arclight.common.mixin.core.world;

import io.izzel.arclight.api.ArclightVersion;
import io.izzel.arclight.common.bridge.world.WorldBridge;
import io.izzel.arclight.common.bridge.world.border.WorldBorderBridge;
import io.izzel.arclight.common.bridge.world.storage.DerivedWorldInfoBridge;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.profiler.IProfiler;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.chunk.AbstractChunkProvider;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.dimension.Dimension;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.WorldInfo;
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
import java.util.function.BiFunction;

@Mixin(World.class)
public abstract class WorldMixin implements WorldBridge {

    // @formatter:off
    @Shadow @Nullable public TileEntity getTileEntity(BlockPos pos) { return null; }
    @Shadow public abstract BlockState getBlockState(BlockPos pos);
    @Shadow public abstract void setTileEntity(BlockPos pos, @Nullable TileEntity tileEntityIn);
    @Shadow  public Dimension dimension;
    @Shadow public abstract WorldInfo getWorldInfo();
    @Shadow public abstract Dimension getDimension();
    @Shadow public abstract long getSeed();
    @Shadow @Final public WorldInfo worldInfo;
    @Shadow public abstract WorldBorder getWorldBorder();
    @Shadow@Final private WorldBorder worldBorder;
    @Shadow public abstract long getDayTime();
    @Accessor("mainThread") public abstract Thread arclight$getMainThread();
    // @formatter:on

    protected CraftWorld world;
    public boolean pvpMode;
    public boolean keepSpawnInMemory = true;
    public long ticksPerAnimalSpawns;
    public long ticksPerMonsterSpawns;
    public long ticksPerWaterSpawns;
    public long ticksPerAmbientSpawns;
    public boolean populating;
    public org.bukkit.generator.ChunkGenerator generator;
    protected org.bukkit.World.Environment environment;
    public org.spigotmc.SpigotWorldConfig spigotConfig;

    @Inject(method = "<init>(Lnet/minecraft/world/storage/WorldInfo;Lnet/minecraft/world/dimension/DimensionType;Ljava/util/function/BiFunction;Lnet/minecraft/profiler/IProfiler;Z)V", at = @At("RETURN"))
    private void arclight$init(WorldInfo info, DimensionType dimType, BiFunction<World, Dimension, AbstractChunkProvider> provider, IProfiler profilerIn, boolean remote, CallbackInfo ci) {
        if (info instanceof DerivedWorldInfoBridge) {
            ((DerivedWorldInfoBridge) info).bridge$setDimension(dimType);
        }
        spigotConfig = new SpigotWorldConfig(info.getWorldName());
        ((WorldBorderBridge) this.worldBorder).bridge$setWorld((ServerWorld) (Object) this);
        this.ticksPerAnimalSpawns = this.getServer().getTicksPerAnimalSpawns();
        this.ticksPerMonsterSpawns = this.getServer().getTicksPerMonsterSpawns();
        if (ArclightVersion.atLeast(ArclightVersion.v1_15)) {
            this.ticksPerWaterSpawns = this.getServer().getTicksPerWaterSpawns();
            this.ticksPerAmbientSpawns = this.getServer().getTicksPerAmbientSpawns();
        } else {
            this.ticksPerWaterSpawns = 1;
            this.ticksPerAmbientSpawns = 1;
        }
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

    public void arclight$constructor(WorldInfo info, DimensionType dimType, BiFunction<World, Dimension, AbstractChunkProvider> provider, IProfiler profilerIn, boolean remote) {
        throw new RuntimeException();
    }

    public void arclight$constructor(WorldInfo info, DimensionType dimType, BiFunction<World, Dimension, AbstractChunkProvider> provider, IProfiler profilerIn, boolean remote, org.bukkit.generator.ChunkGenerator gen, org.bukkit.World.Environment env) {
        arclight$constructor(info, dimType, provider, profilerIn, remote);
        this.generator = gen;
        this.environment = env;
        bridge$getWorld();
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

    @Inject(method = "markAndNotifyBlock", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/block/BlockState;updateNeighbors(Lnet/minecraft/world/IWorld;Lnet/minecraft/util/math/BlockPos;I)V"))
    private void arclight$callBlockPhysics(BlockPos pos, Chunk chunk, BlockState blockstate, BlockState newState, int flags, CallbackInfo ci) {
        if (this.world != null) {
            BlockPhysicsEvent event = new BlockPhysicsEvent(CraftBlock.at((IWorld) this, pos), CraftBlockData.fromData(newState));
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "notifyNeighbors", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;notifyNeighborsOfStateChange(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/Block;)V"))
    private void arclight$noPhysicsPopulating(BlockPos pos, Block blockIn, CallbackInfo ci) {
        if (populating) {
            ci.cancel();
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
                generator = getServer().getGenerator(getWorldInfo().getWorldName());
            }
            if (environment == null) {
                environment = org.bukkit.World.Environment.getEnvironment(getDimension().getType().getId());
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
