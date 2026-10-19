package io.izzel.arclight.common.mixin.bukkit;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.Lifecycle;
import io.izzel.arclight.common.bridge.bukkit.CraftServerBridge;
import io.izzel.arclight.common.bridge.core.server.level.ServerPlayerBridge;
import io.izzel.arclight.common.bridge.core.server.dedicated.DedicatedServerBridge;
import io.izzel.arclight.common.bridge.core.world.level.storage.LevelStorageSourceBridge;
import io.izzel.arclight.common.bridge.core.world.level.storage.PrimaryLevelDataBridge;
import io.izzel.arclight.common.mod.server.world.BukkitWorldIdentity;
import io.izzel.arclight.common.mod.server.world.WorldCreationContext;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.worldupdate.UpgradeProgress;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import io.izzel.arclight.i18n.ArclightConfig;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.NbtException;
import net.minecraft.nbt.ReportedNbtException;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.WorldLoader;
import net.minecraft.server.dedicated.DedicatedPlayerList;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.dedicated.DedicatedServerProperties;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.players.PlayerList;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.ai.village.VillageSiege;
import net.minecraft.world.entity.npc.CatSpawner;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTraderSpawner;
import net.minecraft.world.level.CustomSpawner;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.PatrolSpawner;
import net.minecraft.world.level.levelgen.PhantomSpawner;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.level.validation.ContentValidationException;
import net.minecrell.terminalconsole.TerminalConsoleAppender;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.command.CraftCommandMap;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.generator.CraftWorldInfo;
import org.bukkit.craftbukkit.scheduler.CraftScheduler;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginLoadOrder;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.SimplePluginManager;
import org.bukkit.scheduler.BukkitWorker;
import org.jline.terminal.Terminal;
import org.spigotmc.SpigotConfig;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

@Mixin(value = CraftServer.class, remap = false)
public abstract class CraftServerMixin implements CraftServerBridge {

    /**
     * @author IzzelAliz
     * @reason Use the runtime level-data accessor instead of the patched field descriptor.
     */
    @Overwrite(remap = false)
    public GameMode getDefaultGameMode() {
        var data = (ServerLevelData) this.console.getLevel(net.minecraft.world.level.Level.OVERWORLD).getLevelData();
        return GameMode.getByValue(data.getGameType().getId());
    }

    /**
     * @author IzzelAliz
     * @reason Use the existing appender terminal instead of the patched server field.
     */
    @Overwrite(remap = false)
    public Terminal getTerminal() {
        return TerminalConsoleAppender.getTerminal();
    }

    // @formatter:off
    @Shadow @Final private CraftCommandMap commandMap;
    @Shadow @Final private SimplePluginManager pluginManager;
    @Shadow @Final protected DedicatedServer console;
    @Shadow @Final @Mutable protected DedicatedPlayerList playerList;
    @Shadow @Final @Mutable private List<CraftPlayer> playerView;
    @Shadow @Final private Map<String, World> worlds;
    @Shadow public int reloadCount;
    @Shadow private org.bukkit.Warning.WarningState warningState;
    @Shadow public org.bukkit.craftbukkit.util.ApiVersion minimumAPI;
    @Shadow private boolean printSaveWarning;
    @Shadow public Set<String> activeCompatibilities;
    @Shadow protected abstract void overrideSpawnLimits();
    @Shadow protected abstract void loadIcon();
    @Shadow protected abstract void loadCompatibilities();
    @Shadow public abstract int getTicksPerSpawns(org.bukkit.entity.SpawnCategory category);
    @Shadow private YamlConfiguration configuration;
    @Shadow protected abstract File getConfigFile();
    @Shadow private YamlConfiguration commandsConfiguration;
    @Shadow protected abstract File getCommandsConfigFile();
    @Shadow@Final private Logger logger;
    @Shadow public abstract void reloadData();
    @Shadow private boolean overrideAllCommandBlockCommands;
    @Shadow public boolean ignoreVanillaPermissions;
    @Shadow public abstract CraftScheduler getScheduler();
    @Shadow public abstract Logger getLogger();
    @Shadow public abstract void loadPlugins();
    @Shadow public abstract void enablePlugins(PluginLoadOrder type);
    @Shadow public abstract PluginManager getPluginManager();
    @Accessor("logger") @Mutable public abstract void setLogger(Logger logger);
    @Shadow public abstract ChunkGenerator getGenerator(String world);
    @Shadow public abstract BiomeProvider getBiomeProvider(String world);
    // @formatter:on

    @org.spongepowered.asm.mixin.injection.Redirect(method = "craftItemResult([Lorg/bukkit/inventory/ItemStack;Lorg/bukkit/World;Lorg/bukkit/entity/Player;)Lorg/bukkit/inventory/ItemCraftResult;",
        at = @At(value = "FIELD", target = "Lnet/minecraft/world/inventory/CraftingMenu;craftSlots:Lnet/minecraft/world/inventory/TransientCraftingContainer;", opcode = org.objectweb.asm.Opcodes.GETFIELD, remap = false),
        remap = false, require = 1, expect = 1, allow = 1)
    private net.minecraft.world.inventory.TransientCraftingContainer arclight$readNativeCraftSlots(net.minecraft.world.inventory.CraftingMenu menu) {
        // Keep the patched caller's stack type, but read the native interface field.
        return (net.minecraft.world.inventory.TransientCraftingContainer) ((net.minecraft.world.inventory.AbstractCraftingMenu) menu).craftSlots;
    }

    @Shadow
    public abstract File getWorldContainer();

    @Shadow
    public abstract World getWorld(String name);

    @Shadow
    public abstract World getWorld(UUID uid);

    @Shadow
    public abstract DedicatedServer getServer();

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite(remap = false)
    public String getName() {
        return "Arclight";
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public String getVersion() {
        return System.getProperty("arclight.version") + " (MC: " + this.console.getServerVersion() + ")";
    }

    @Override
    public void bridge$setPlayerList(PlayerList playerList) {
        // Some plugin may change to a different PlayerList
        this.playerList = (DedicatedPlayerList) playerList;
        this.playerView = Collections.unmodifiableList(Lists.transform(playerList.players, player ->
                ((ServerPlayerBridge)player).bridge$getBukkitEntity()
                ));
    }

    @Inject(method = "dispatchCommand", remap = false, cancellable = true, at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lorg/spigotmc/AsyncCatcher;catchOp(Ljava/lang/String;)V"))
    private void arclight$returnIfFail(CommandSender sender, String commandLine, CallbackInfoReturnable<Boolean> cir) {
        if (commandLine == null) {
            cir.setReturnValue(false);
        }
    }

    @Override
    public void bridge$removeWorld(ServerLevel world) {
        if (world == null) {
            return;
        }
        this.worlds.remove(world.bridge$getWorld().getName().toLowerCase(Locale.ROOT), world.bridge$getWorld());
    }

    @org.spongepowered.asm.mixin.injection.Redirect(method = "unloadWorld(Lorg/bukkit/World;Z)Z",
        at = @At(value = "FIELD", target = "Lnet/minecraft/server/level/ServerLevel;storageSource:Lnet/minecraft/world/level/storage/LevelStorageSource$LevelStorageAccess;", opcode = org.objectweb.asm.Opcodes.GETFIELD),
        remap = false, require = 1, expect = 1, allow = 1)
    private LevelStorageSource.LevelStorageAccess arclight$unloadStorageAccess(ServerLevel level) {
        return ((io.izzel.arclight.common.bridge.core.server.level.ServerLevelBridge) level).bridge$getConvertable();
    }

    @org.spongepowered.asm.mixin.injection.Redirect(method = "unloadWorld(Lorg/bukkit/World;Z)Z",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/storage/LevelStorageSource$LevelStorageAccess;close()V"),
        remap = false, require = 1, expect = 1, allow = 1)
    private void arclight$unloadCloseStorage(LevelStorageSource.LevelStorageAccess access) throws IOException {
        // Typed accesses belong to explicitly created Bukkit worlds. Native dimensions
        // share the untyped server access; unloading one must not release its siblings' lock.
        if (((LevelStorageSourceBridge.LevelStorageAccessBridge) access).bridge$getTypeKey() != null) {
            access.close();
        }
    }

    @Inject(method = "unloadWorld(Lorg/bukkit/World;Z)Z", at = @At("RETURN"), remap = false)
    private void arclight$discardUnloadedWorld(World world, boolean save, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() && world instanceof CraftWorld) {
            WorldCreationContext.discard(((CraftWorld) world).getHandle());
        }
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite(remap = false)
    public void reload() {
        ++this.reloadCount;
        this.configuration = YamlConfiguration.loadConfiguration(this.getConfigFile());
        this.commandsConfiguration = YamlConfiguration.loadConfiguration(this.getCommandsConfigFile());

        DedicatedServerProperties config = ((DedicatedServerBridge) this.console).bridge$reloadProperties();
        ((io.izzel.arclight.common.bridge.core.server.MinecraftServerBridge) this.console)
            .bridge$setAutosavePeriod(this.configuration.getInt("ticks-per.autosave"));
        ((io.izzel.arclight.common.bridge.core.server.level.TicketTypeBridge) (Object) net.minecraft.server.level.TicketType.FORCED)
            .bridge$setPluginTimeout(this.configuration.getInt("chunk-gc.period-in-ticks"));
        this.overrideSpawnLimits();
        this.warningState = org.bukkit.Warning.WarningState.value(this.configuration.getString("settings.deprecated-verbose"));
        this.minimumAPI = org.bukkit.craftbukkit.util.ApiVersion.getOrCreateVersion(this.configuration.getString("settings.minimum-api"));
        this.printSaveWarning = false;
        this.loadIcon();
        this.loadCompatibilities();
        org.bukkit.craftbukkit.util.CraftMagicNumbers.INSTANCE.getCommodore().updateReroute(this.activeCompatibilities::contains);
        SpigotConfig.init((File) ((io.izzel.arclight.common.bridge.core.server.MinecraftServerBridge) this.console).bridge$getOptions().valueOf("spigot-settings"));
        for (ServerLevel level : this.console.getAllLevels()) {
            ((io.izzel.arclight.common.bridge.core.server.level.ServerLevelBridge) level).bridge$getPrimaryLevelData().setDifficulty(config.difficulty.get());
            var bridge = (io.izzel.arclight.common.bridge.core.world.level.WorldBridge) level;
            for (org.bukkit.entity.SpawnCategory category : org.bukkit.entity.SpawnCategory.values()) {
                if (org.bukkit.craftbukkit.util.CraftSpawnCategory.isValidForLimits(category)) {
                    long ticks = this.getTicksPerSpawns(category);
                    bridge.bridge$ticksPerSpawnCategory().put(category, ticks < 0
                        ? org.bukkit.craftbukkit.util.CraftSpawnCategory.getDefaultTicksPerSpawn(category) : ticks);
                }
            }
            bridge.bridge$spigotConfig().init();
        }

        try {
            this.playerList.getIpBans().load();
        } catch (IOException var12) {
            this.logger.log(Level.WARNING, "Failed to load banned-ips.json, " + var12.getMessage());
        }

        try {
            this.playerList.getBans().load();
        } catch (IOException var11) {
            this.logger.log(Level.WARNING, "Failed to load banned-players.json, " + var11.getMessage());
        }

        this.pluginManager.clearPlugins();
        this.commandMap.clearCommands();
        this.reloadData();
        SpigotConfig.registerCommands();
        this.overrideAllCommandBlockCommands = this.commandsConfiguration.getStringList("command-block-overrides").contains("*");
        this.ignoreVanillaPermissions = this.commandsConfiguration.getBoolean("ignore-vanilla-permissions");

        for (int pollCount = 0; pollCount < 50 && this.getScheduler().getActiveWorkers().size() > 0; ++pollCount) {
            try {
                Thread.sleep(50L);
            } catch (InterruptedException var10) {
            }
        }

        List<BukkitWorker> overdueWorkers = this.getScheduler().getActiveWorkers();

        for (BukkitWorker worker : overdueWorkers) {
            Plugin plugin = worker.getOwner();
            this.getLogger().log(Level.SEVERE, String.format("Nag author(s): '%s' of '%s' about the following: %s", plugin.getDescription().getAuthors(), plugin.getDescription().getFullName(), "This plugin is not properly shutting down its async tasks when it is being reloaded.  This may cause conflicts with the newly loaded version of the plugin"));
        }

        this.loadPlugins();
        this.enablePlugins(PluginLoadOrder.STARTUP);
        this.enablePlugins(PluginLoadOrder.POSTWORLD);
        this.getPluginManager().callEvent(new ServerLoadEvent(ServerLoadEvent.LoadType.RELOAD));
    }

    private final Map<String, ChunkGenerator> generatorCache = new HashMap<>();
    private final Map<String, BiomeProvider> biomeProviderCache = new HashMap<>();
    private final Map<String, World.Environment> environmentCache = new HashMap<>();

    @Override
    public void bridge$offerGeneratorCache(String name, ChunkGenerator generator) {
        // Newly created level
        generatorCache.put(name, generator);
    }

    @Override
    public ChunkGenerator bridge$consumeGeneratorCache(String name) {
        var cache = generatorCache.remove(name);
        if (cache == null) {
            // If not provided (which means it's not newly created),
            // load from bukkit.yml configuration.
            // See CraftServer
            cache = getGenerator(name);
        }
        return cache;
    }

    @Override
    public BiomeProvider bridge$consumeBiomeProviderCache(String name) {
        var cache = biomeProviderCache.remove(name);
        if (cache == null) {
            // If not provided (which means it's not newly created),
            // load from bukkit.yml configuration.
            // See CraftServer
            cache = getBiomeProvider(name);
        }
        return cache;
    }

    @Override
    public void bridge$offerBiomeProviderCache(String name, BiomeProvider provider) {
        // Newly created level
        biomeProviderCache.put(name, provider);
    }

    @Override
    public World.Environment bridge$consumeEnvironmentCache(String name) {
        return environmentCache.remove(name);
    }

    @Override
    public void bridge$offerEnvironmentCache(String name, World.Environment environment) {
        environmentCache.put(name, environment);
    }

    /**
     * @author InitAuther97
     * @reason experimental base generator setting & support for CUSTOM environment
     */
    @Overwrite
    public World createWorld(WorldCreator creator) {
        Preconditions.checkState(this.console.getAllLevels().iterator().hasNext(), "Cannot create additional worlds on STARTUP");
        Preconditions.checkArgument(creator != null, "WorldCreator cannot be null");
        String name = creator.name();
        ChunkGenerator generator = creator.generator();
        BiomeProvider biomeProvider = creator.biomeProvider();
        File folder = new File(this.getWorldContainer(), name);
        World world = this.getWorld(name);
        if (world != null) {
            return world;
        } else {
            if (folder.exists()) {
                Preconditions.checkArgument(folder.isDirectory(), "File (%s) exists and isn't a folder", name);
            }

            if (generator == null) {
                generator = this.getGenerator(name);
            }

            if (biomeProvider == null) {
                biomeProvider = this.getBiomeProvider(name);
            }

            // Arclight start: handle CUSTOM
            ResourceKey<LevelStem> actualDimension;
            boolean isCustom = false;
            switch (creator.environment()) {
                case NORMAL -> actualDimension = LevelStem.OVERWORLD;
                case NETHER -> actualDimension = LevelStem.NETHER;
                case THE_END -> actualDimension = LevelStem.END;
                case CUSTOM -> {
                    if (ArclightConfig.spec().getExperimental().canOverrideWorldgen()) {
                        isCustom = true;
                        final var location = Identifier.tryBuild("bukkit", name);
                        if (location == null) {
                            throw new IllegalArgumentException("Illegal world name: " + name);
                        }
                        actualDimension = ResourceKey.create(Registries.LEVEL_STEM, location);
                    } else {
                        throw new IllegalArgumentException("Illegal dimension (" + creator.environment() + ")");
                    }
                }
                default -> throw new IllegalArgumentException("Illegal dimension (" + creator.environment() + ")");
            }
            // Arclight end

            LevelStorageSource.LevelStorageAccess worldSession;
            try {
                worldSession = ((LevelStorageSourceBridge) LevelStorageSource.createDefault(this.getWorldContainer().toPath())).arclight$validateAndCreateAccess(name, actualDimension);
            } catch (ContentValidationException | IOException ex) {
                throw new RuntimeException(ex);
            }

            boolean added = false;
            Throwable failure = null;
            // Session ownership boundary: retain storage for a successfully published world.
            try {
                WorldLoader.DataLoadContext context = ((DedicatedServerBridge) this.console).arclight$dataLoadContext();
                Dynamic<?> dynamic;
                if (worldSession.hasWorldData()) {
                    try {
                        dynamic = worldSession.getUnfixedDataTagWithFallback();
                    } catch (ReportedNbtException | IOException | NbtException ioexception) {
                        MinecraftServer.LOGGER.error("Failed to load world data. World files may be corrupted. Shutting down.", ioexception);
                        return null;
                    }
                    LevelSummary worldinfo = worldSession.fixAndGetSummaryFromTag(dynamic);
                    if (worldinfo.requiresManualConversion()) {
                        MinecraftServer.LOGGER.info("This world must be opened in an older version (like 1.6.4) to be safely converted");
                        return null;
                    }
                    if (!worldinfo.isCompatible()) {
                        MinecraftServer.LOGGER.info("This world was created by an incompatible version.");
                        return null;
                    }
                    dynamic = DataFixers.getFileFixer().fix(worldSession, dynamic, new UpgradeProgress());
                    // FileFixer at version 4772 migrates legacy level.dat WorldGenSettings
                    // into data/minecraft/world_gen_settings.dat before this check. Missing after
                    // that upgrade means recovery is not proven safe: never accept random fallback.
                    var savedWorldgen = LevelStorageSource.readExistingSavedData(worldSession, context.datapackWorldgen(), WorldGenSettings.TYPE);
                    if (savedWorldgen.result().isEmpty()) {
                        throw new IllegalStateException("Missing or invalid world generation settings for " + name
                            + " (data/minecraft/world_gen_settings.dat); refusing to substitute a random seed: " + savedWorldgen.error());
                    }
                } else {
                    dynamic = null;
                }

                boolean hardcore = creator.hardcore();
                RegistryAccess.Frozen datapackDimensions = context.datapackDimensions();
                Registry<LevelStem> datapackStems = datapackDimensions.lookupOrThrow(Registries.LEVEL_STEM);
                RegistryAccess.Frozen dimensions;
                PrimaryLevelData levelData;
                LevelDataAndDimensions.WorldDataAndGenSettings dataAndSettings;
                if (dynamic != null) {
                    LevelDataAndDimensions levelDataAndDimensions = LevelStorageSource.getLevelDataAndDimensions(worldSession, dynamic, context.dataConfiguration(), datapackStems, context.datapackWorldgen());
                    dataAndSettings = levelDataAndDimensions.worldDataAndGenSettings();
                    levelData = (PrimaryLevelData) dataAndSettings.data();
                    // For CUSTOM, the server's experimental stem registry supplies the stem;
                    // loaded generation settings/options still belong to this world's files.
                    dimensions = isCustom ? this.console.registries().getLayer(RegistryLayer.DIMENSIONS)
                        : levelDataAndDimensions.dimensions().dimensionsRegistryAccess();
                } else {
                    WorldOptions options = new WorldOptions(creator.seed(), creator.generateStructures(), false);
                    LevelSettings settings = new LevelSettings(name, GameType.byId(this.getDefaultGameMode().getValue()), new LevelSettings.DifficultySettings(Difficulty.EASY, hardcore, false), false, context.dataConfiguration());
                    WorldDimensions worldDimensions;
                    if (isCustom) {
                        DedicatedServerProperties.WorldDimensionData properties = new DedicatedServerProperties.WorldDimensionData(GsonHelper.parse(creator.generatorSettings().isEmpty() ? "{}" : creator.generatorSettings()), creator.type().name().toLowerCase(Locale.ROOT));
                        worldDimensions = properties.create(context.datapackWorldgen());
                        WorldDimensions.Complete baked = worldDimensions.bake(datapackStems);
                        Lifecycle lifecycle = baked.lifecycle().add(context.datapackWorldgen().allRegistriesLifecycle());
                        levelData = new PrimaryLevelData(settings, baked.specialWorldProperty(), lifecycle);
                        dimensions = baked.dimensionsRegistryAccess();
                    } else {
                        WorldData template = this.console.getWorldData();
                        final PrimaryLevelData.SpecialWorldProperty property;
                        if (template.isDebugWorld()) {
                            property = PrimaryLevelData.SpecialWorldProperty.DEBUG;
                        } else if (template.isFlatWorld()) {
                            property = PrimaryLevelData.SpecialWorldProperty.FLAT;
                        } else {
                            property = PrimaryLevelData.SpecialWorldProperty.NONE;
                        }
                        levelData = new PrimaryLevelData(settings, property, template.worldGenSettingsLifecycle());
                        dimensions = this.console.registries().getLayer(RegistryLayer.DIMENSIONS);
                        worldDimensions = new WorldDimensions(dimensions.lookupOrThrow(Registries.LEVEL_STEM));
                    }
                    dataAndSettings = new LevelDataAndDimensions.WorldDataAndGenSettings(levelData, new WorldGenSettings(options, worldDimensions));
                }
                WorldOptions options = dataAndSettings.genSettings().options();

                Registry<LevelStem> stems = dimensions.lookupOrThrow(Registries.LEVEL_STEM);
                LevelStem stem = stems.getValue(actualDimension);
                if (stem == null) {
                    throw new IllegalArgumentException("Unknown level stem: " + actualDimension);
                }
                if (actualDimension != null) {
                    ((PrimaryLevelDataBridge) levelData).arclight$offerCustomDimensions(stems);
                }
                ((PrimaryLevelDataBridge) levelData).arclight$checkName(name);
                levelData.setModdedInfo(this.console.getServerModName(), this.console.getModdedStatus().shouldReportAsModified());

                ((DedicatedServerBridge) this.console).arclight$forceUpgradeIfNeeded(worldSession, levelData, dimensions); // Arclight

                // WorldGenSettings.TYPE uses data/minecraft/world_gen_settings.dat.
                // Persist before publishing the world: otherwise its seed silently randomizes on reopen.
                if (dynamic == null) {
                    try {
                        worldSession.saveDataTag(levelData);
                        RegistryAccess.Frozen worldgenForSave = new RegistryAccess.ImmutableRegistryAccess(
                            java.util.stream.Stream.concat(
                                this.console.registries().getAccessForLoading(RegistryLayer.DIMENSIONS).registries(),
                                dimensions.registries()
                            )
                        ).freeze();
                        LevelStorageSource.writeWorldGenSettings(worldgenForSave, worldSession.getLevelPath(LevelResource.ROOT), dataAndSettings.genSettings());
                    } catch (IOException ex) {
                        throw new RuntimeException("Unable to persist world generation settings for " + name, ex);
                    }
                }
                SavedDataStorage dataStorage = new SavedDataStorage(worldSession.getLevelPath(LevelResource.DATA), this.console.getFixerUpper(), this.console.registryAccess());
                // Reopened saves own their generation options; a new creator seed must not
                // change the biome source for already-generated neighboring chunks.
                long j = BiomeManager.obfuscateSeed(options.seed());
                List<CustomSpawner> list = ImmutableList.of(new PhantomSpawner(), new PatrolSpawner(), new CatSpawner(), new VillageSiege(), new WanderingTraderSpawner(dataStorage));
                UUID worldUid = BukkitWorldIdentity.getUUID(worldSession, ResourceKey.create(Registries.DIMENSION, actualDimension.identifier()));
                DimensionType dimensionType = stem.type().value();
                WorldInfo worldInfo = new CraftWorldInfo(levelData.getLevelName(), worldUid, creator.environment(), options.seed(), dimensionType.minY(), dimensionType.minY() + dimensionType.height());
                if (biomeProvider == null && generator != null) {
                    biomeProvider = generator.getDefaultBiomeProvider(worldInfo);
                }

                String levelName = this.console.getProperties().levelName;
                ResourceKey<net.minecraft.world.level.Level> worldKey;
                if (name.equals(levelName + "_nether")) {
                    worldKey = net.minecraft.world.level.Level.NETHER;
                } else if (name.equals(levelName + "_the_end")) {
                    worldKey = net.minecraft.world.level.Level.END;
                } else {
                    worldKey = ResourceKey.create(Registries.DIMENSION, Identifier.withDefaultNamespace(name.toLowerCase(Locale.ROOT)));
                }

                // Duplicate UID is rejected by CraftServer.addWorld during the constructor.
                // Reject before construction so no unowned level/storage is allocated.
                World duplicate = this.getWorld(worldUid);
                if (duplicate != null) {
                    MinecraftServer.LOGGER.warn("Refusing to create world {}: UUID {} is already used by {}", name, worldUid, duplicate.getName());
                    return null;
                }

                this.bridge$offerBiomeProviderCache(name, biomeProvider);
                this.bridge$offerGeneratorCache(name, generator);
                this.bridge$offerEnvironmentCache(name, creator.environment());
                ServerLevel internal = null;
                WorldCreationContext.begin(dataAndSettings.genSettings());
                try {
                    internal = new ServerLevel(this.console, this.console.executor, worldSession, levelData, worldKey, stem, levelData.isDebugWorld(), j, (List)(creator.environment() == World.Environment.NORMAL ? list : ImmutableList.of()), true);
                    WorldCreationContext.attach(internal);
                    if (this.worlds.get(name.toLowerCase(Locale.ROOT)) != internal.bridge$getWorld()) {
                        return null; // duplicate UID/name was rejected in constructor; frame cleanup discards it
                    }
                    ((DedicatedServerBridge) this.console).arclight$prepareAndAddLevel(internal, levelData);
                    CraftWorld bukkit = internal.bridge$getWorld();
                    this.pluginManager.callEvent(new WorldLoadEvent(bukkit));
                    WorldCreationContext.complete(internal);
                    added = true;
                    return bukkit;
                } catch (RuntimeException | Error ex) {
                    failure = ex;
                    throw ex;
                } finally {
                    Throwable cleanupFailure = null;
                    try {
                        if (!added) {
                            if (internal != null) {
                                try {
                                    // addLevel publishes before notifying loaders. The callback itself
                                    // may throw, or a nested creation may replace the dimension.
                                    ((io.izzel.arclight.common.bridge.core.server.MinecraftServerBridge) this.console)
                                        .arclight$rollbackPublishedLevel(internal);
                                } catch (RuntimeException | Error ex) {
                                    cleanupFailure = ex;
                                }
                            }
                            // addWorld can publish during ServerLevel construction, before it returns.
                            // Remove a world published before a failed constructor returned.
                            World registered = this.worlds.get(name.toLowerCase(Locale.ROOT));
                            if (registered instanceof CraftWorld && WorldCreationContext.ownsPending(((CraftWorld) registered).getHandle())
                                && (internal == null || ((CraftWorld) registered).getHandle() == internal)) {
                                this.worlds.remove(name.toLowerCase(Locale.ROOT), registered);
                            }
                            if (internal != null) {
                                WorldCreationContext.discard(internal);
                            }
                            this.biomeProviderCache.remove(name);
                            this.generatorCache.remove(name);
                            this.environmentCache.remove(name);
                        }
                    } catch (RuntimeException | Error ex) {
                        if (cleanupFailure == null) cleanupFailure = ex;
                        else if (cleanupFailure != ex) cleanupFailure.addSuppressed(ex);
                    } finally {
                        try {
                            WorldCreationContext.clearPending();
                        } catch (RuntimeException | Error ex) {
                            if (cleanupFailure == null) cleanupFailure = ex;
                            else if (cleanupFailure != ex) cleanupFailure.addSuppressed(ex);
                        }
                    }
                    if (cleanupFailure != null) {
                        if (failure != null) {
                            if (failure != cleanupFailure) failure.addSuppressed(cleanupFailure);
                        } else if (cleanupFailure instanceof Error error) {
                            throw error;
                        } else {
                            throw (RuntimeException) cleanupFailure;
                        }
                    }
                }
            } catch (RuntimeException | Error ex) {
                failure = ex;
                throw ex;
            } finally {
                if (!added) {
                    try {
                        worldSession.close();
                    } catch (IOException ex) {
                        if (failure != null) {
                            if (failure != ex) failure.addSuppressed(ex);
                        } else {
                            MinecraftServer.LOGGER.warn("Failed to close world storage for {}", name, ex);
                        }
                    } catch (RuntimeException | Error ex) {
                        if (failure != null) {
                            if (failure != ex) failure.addSuppressed(ex);
                        } else {
                            throw ex;
                        }
                    }
                }
            }
        }
    }
}
