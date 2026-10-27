package io.izzel.arclight.common.mixin.core.server.level;

import io.izzel.arclight.common.bridge.core.server.level.ServerLevelSavedDataBridge;
import io.izzel.arclight.common.bridge.core.server.level.ServerLevelBridge;
import io.izzel.arclight.common.mod.server.world.WorldCreationContext;
import io.izzel.arclight.common.mod.server.world.ClockLegacyImport;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource;
import java.io.IOException;
import java.nio.file.Path;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.bossevents.CustomBossEvents;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomSequences;
import net.minecraft.world.clock.ServerClockManager;
import io.izzel.arclight.common.bridge.core.world.clock.ServerClockOwnerBridge;
import net.minecraft.world.level.gamerules.GameRuleMap;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.storage.SavedDataStorage;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.objectweb.asm.Opcodes;

/** World-owned saved state; the server's own global SavedData remains untouched. */
@Mixin(ServerLevel.class)
public abstract class ServerLevelSavedDataMixin implements ServerLevelSavedDataBridge {

    @Shadow @Final private ServerChunkCache chunkSource;
    @Shadow public abstract MinecraftServer getServer();
    @Shadow public abstract SavedDataStorage getDataStorage();

    private ServerClockManager arclight$clockManager;
    private GameRules arclight$gameRules;
    private RandomSequences arclight$randomSequences;
    private CustomBossEvents arclight$customBossEvents;

    /** The native global manager is initialized before this level. Import only its
     * already-decoded state, never raw-copy pre-DFU bytes or reinit that manager.
     * This runs before the world's storage first loads TYPE (and caches misses).
     */
    private void arclight$prepareClockImport(LevelStorageSource.LevelStorageAccess access) {
        if (!((ServerLevel) (Object) this).dimension().equals(Level.OVERWORLD)) {
            return;
        }
        MinecraftServer server = this.getServer();
        Path legacy = ServerClockManager.TYPE.id().withSuffix(".dat")
            .resolveAgainst(server.getWorldPath(LevelResource.DATA));
        Path target = ServerClockManager.TYPE.id().withSuffix(".dat")
            .resolveAgainst(access.getDimensionPath(Level.OVERWORLD).resolve("data"));
        try {
            if (ClockLegacyImport.regularFileExists(target)) {
                // get() normally catches parse errors and returns null. An existing bad
                // target must fail closed rather than silently fall back to old global data.
                arclight$readClockStrict(target);
            } else if (ClockLegacyImport.regularFileExists(legacy)) {
                // The global get() swallows malformed NBT/codec errors. Do not turn a
                // broken old save into a fresh empty world clock by encoding that fallback.
                arclight$readClockStrict(legacy);
                ServerClockManager global = server.clockManager();
                ClockLegacyImport.publishIfAbsent(target, temporary -> {
                    CompoundTag tag = new CompoundTag();
                    var ops = server.registryAccess().createSerializationContext(NbtOps.INSTANCE);
                    tag.put("data", ServerClockManager.TYPE.codec().encodeStart(ops, global).getOrThrow());
                    NbtUtils.addCurrentDataVersion(tag);
                    NbtIo.writeCompressed(tag, temporary);
                });
                arclight$readClockStrict(target);
            }
        } catch (Exception failure) {
            throw new IllegalStateException("Cannot load or import world clock saved data: " + legacy + " -> " + target, failure);
        }
    }

    private void arclight$readClockStrict(Path target) throws IOException {
        // Reuse native gzip/uncompressed NBT + DataFixTypes.SAVED_DATA_WORLD_CLOCKS
        // instead of bypassing the file fixer with a raw copy.
        CompoundTag tag = this.getDataStorage().readTagFromDisk(target,
            ServerClockManager.TYPE.dataFixType(), SharedConstants.getCurrentVersion().dataVersion().version());
        RegistryOps<net.minecraft.nbt.Tag> ops = this.getServer().registryAccess().createSerializationContext(NbtOps.INSTANCE);
        ServerClockManager.TYPE.codec().parse(ops, tag.get("data")).getOrThrow();
    }

    // The world storage becomes accessible only after chunkSource is assigned.
    // Import at that precise point: vanilla and Neo constructor code below it can
    // call clockManager() while updating sky brightness/initializing attachments.
    // A RETURN hook is too late: computeIfAbsent may already have cached an empty
    // world clock, so publishing the old file then would silently lose the import.
    @io.izzel.arclight.mixin.Decorate(method = "<init>", inject = true, at = @At(value = "FIELD",
        target = "Lnet/minecraft/server/level/ServerLevel;chunkSource:Lnet/minecraft/server/level/ServerChunkCache;",
        opcode = Opcodes.PUTFIELD, shift = At.Shift.AFTER))
    private void arclight$importClockBeforeWorldInitialization(MinecraftServer server, java.util.concurrent.Executor executor,
            LevelStorageSource.LevelStorageAccess access, net.minecraft.world.level.storage.ServerLevelData levelData,
            net.minecraft.resources.ResourceKey<Level> dimension, net.minecraft.world.level.dimension.LevelStem stem,
            boolean debug, long seed, java.util.List<net.minecraft.world.level.CustomSpawner> spawners,
            boolean ticking) {
        if (!io.papermc.paper.configuration.GlobalConfiguration.get().time.affectsAllWorlds) {
            this.arclight$prepareClockImport(access);
            this.bridge$getClockManager();
        }
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void arclight$loadWorldSavedData(MinecraftServer server, java.util.concurrent.Executor executor,
            LevelStorageSource.LevelStorageAccess access, net.minecraft.world.level.storage.ServerLevelData levelData,
            net.minecraft.resources.ResourceKey<Level> dimension, net.minecraft.world.level.dimension.LevelStem stem,
            boolean debug, long seed, java.util.List<net.minecraft.world.level.CustomSpawner> spawners,
            boolean ticking, CallbackInfo ci) {
        this.bridge$getGameRules();
        this.bridge$getRandomSequences();
        this.bridge$getCustomBossEvents();
    }

    public ServerClockManager bridge$getClockManager() {
        if (io.papermc.paper.configuration.GlobalConfiguration.get().time.affectsAllWorlds) {
            return this.getServer().clockManager();
        }
        if (this.arclight$clockManager == null) {
            ServerClockManager manager = this.getDataStorage().computeIfAbsent(ServerClockManager.TYPE);
            ((ServerClockOwnerBridge) manager).init((ServerLevel) (Object) this);
            this.arclight$clockManager = manager;
        }
        return this.arclight$clockManager;
    }

    @Inject(method = "clockManager", at = @At("HEAD"), cancellable = true)
    private void arclight$worldClock(CallbackInfoReturnable<ServerClockManager> cir) {
        if (this.chunkSource != null) {
            cir.setReturnValue(io.papermc.paper.configuration.GlobalConfiguration.get().time.affectsAllWorlds
                ? this.getServer().clockManager() : this.arclight$clockManager);
        }
    }

    @Override
    public GameRules bridge$getGameRules() {
        if (this.arclight$gameRules == null) {
            if (this.chunkSource == null) {
                return this.getServer().getGameRules();
            }
            GameRuleMap rules = this.getDataStorage().computeIfAbsent(GameRuleMap.TYPE);
            // The map is the level's own persisted object: copying it would break dirty
            // tracking, and importing the global map would replace existing world values.
            this.arclight$gameRules = new GameRules(((ServerLevelBridge) this).bridge$getPrimaryLevelData().enabledFeatures(), rules);
        }
        return this.arclight$gameRules;
    }

    @Inject(method = "getGameRules", at = @At("HEAD"), cancellable = true)
    private void arclight$worldGameRules(CallbackInfoReturnable<GameRules> cir) {
        if (this.chunkSource != null) {
            cir.setReturnValue(this.bridge$getGameRules());
        }
    }

    @Override
    public RandomSequences bridge$getRandomSequences() {
        if (this.arclight$randomSequences == null) {
            this.arclight$randomSequences = this.getDataStorage().computeIfAbsent(RandomSequences.TYPE);
        }
        return this.arclight$randomSequences;
    }

    // Mirror fixed CraftBukkit ServerLevel's per-world WorldGenSettings seed. A
    // Bukkit-created world can have a different persisted seed from the server.
    private WorldGenSettings bridge$getWorldGenSettings() {
        var own = WorldCreationContext.lookup((ServerLevel) (Object) this);
        return own != null ? own : this.getServer().getWorldGenSettings();
    }

    @Override
    public RandomSource bridge$getRandomSequence(Identifier key) {
        return this.bridge$getRandomSequences().get(key, this.bridge$getWorldGenSettings().options().seed());
    }

    public RandomSource getRandomSequence(Identifier key) {
        return this.bridge$getRandomSequence(key);
    }

    public RandomSequences getRandomSequences() {
        return this.bridge$getRandomSequences();
    }

    @Override
    public CustomBossEvents bridge$getCustomBossEvents() {
        if (this.arclight$customBossEvents == null) {
            this.arclight$customBossEvents = this.getDataStorage().computeIfAbsent(CustomBossEvents.TYPE);
        }
        return this.arclight$customBossEvents;
    }

    public CustomBossEvents getCustomBossEvents() {
        return this.bridge$getCustomBossEvents();
    }
}
