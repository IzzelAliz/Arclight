package io.izzel.arclight.common.mixin.core.world.level.chunk;

import io.izzel.arclight.common.bridge.core.world.WorldBridge;
import io.izzel.arclight.common.bridge.core.world.chunk.ChunkAccessBridge;
import io.izzel.arclight.common.bridge.core.world.chunk.ChunkBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.levelgen.blending.BlendingData;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.ticks.LevelChunkTicks;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.CraftChunk;
import org.bukkit.craftbukkit.v.persistence.CraftPersistentDataContainer;
import org.bukkit.event.world.ChunkLoadEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

@Mixin(LevelChunk.class)
public abstract class LevelChunkMixin extends ChunkAccessMixin implements ChunkBridge {

    // @formatter:off
    @Shadow @Nullable public abstract BlockState setBlockState(BlockPos pos, BlockState state, boolean isMoving);
    @Shadow @Final public Level level;
    // @formatter:on

    public org.bukkit.Chunk bukkitChunk;
    public boolean mustNotSave;
    public boolean needsDecoration;
    private transient boolean arclight$doPlace;
    public ServerLevel $$level;

    @Inject(method = "<init>(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/ChunkPos;Lnet/minecraft/world/level/chunk/UpgradeData;Lnet/minecraft/world/ticks/LevelChunkTicks;Lnet/minecraft/world/ticks/LevelChunkTicks;J[Lnet/minecraft/world/level/chunk/LevelChunkSection;Lnet/minecraft/world/level/chunk/LevelChunk$PostLoadProcessor;Lnet/minecraft/world/level/levelgen/blending/BlendingData;)V", at = @At("RETURN"))
    private void arclight$init(Level worldIn, ChunkPos p_196855_, UpgradeData p_196856_, LevelChunkTicks<Block> p_196857_, LevelChunkTicks<Fluid> p_196858_, long p_196859_, @Nullable LevelChunkSection[] p_196860_, @Nullable LevelChunk.PostLoadProcessor p_196861_, @Nullable BlendingData p_196862_, CallbackInfo ci) {
        this.$$level = ((ServerLevel) worldIn);
        this.bukkitChunk = new CraftChunk((LevelChunk) (Object) this);
    }

    @Inject(method = "<init>(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/chunk/ProtoChunk;Lnet/minecraft/world/level/chunk/LevelChunk$PostLoadProcessor;)V", at = @At("RETURN"))
    private void arclight$init(ServerLevel p_196850_, ProtoChunk protoChunk, @Nullable LevelChunk.PostLoadProcessor p_196852_, CallbackInfo ci) {
        this.needsDecoration = true;
        this.persistentDataContainer = (CraftPersistentDataContainer) ((ChunkAccessBridge) protoChunk).bridge$getPersistentDataContainer();
    }

    @Inject(method = "removeBlockEntity", at = @At(value = "INVOKE_ASSIGN", remap = false, target = "Ljava/util/Map;remove(Ljava/lang/Object;)Ljava/lang/Object;"))
    private void arclight$remove(BlockPos pos, CallbackInfo ci) {
        if (!pendingBlockEntities.isEmpty()) {
            pendingBlockEntities.remove(pos);
        }
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

    public BlockState setBlockState(BlockPos pos, BlockState state, boolean isMoving, boolean doPlace) {
        this.arclight$doPlace = doPlace;
        try {
            return this.setBlockState(pos, state, isMoving);
        } finally {
            this.arclight$doPlace = true;
        }
    }

    @Override
    public BlockState bridge$setType(BlockPos pos, BlockState state, boolean isMoving, boolean doPlace) {
        return this.setBlockState(pos, state, isMoving, doPlace);
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

    @Redirect(method = "setBlockState", at = @At(value = "FIELD", ordinal = 1, target = "Lnet/minecraft/world/level/Level;isClientSide:Z"))
    public boolean arclight$redirectIsRemote(Level world) {
        return world.isClientSide && this.arclight$doPlace;
    }

    @Override
    public boolean isUnsaved() {
        return super.isUnsaved() && !this.mustNotSave;
    }
}
