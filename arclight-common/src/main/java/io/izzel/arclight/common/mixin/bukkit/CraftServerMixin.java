package io.izzel.arclight.common.mixin.bukkit;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.Lifecycle;
import io.izzel.arclight.common.bridge.bukkit.CraftServerBridge;
import io.izzel.arclight.common.bridge.core.server.level.ServerPlayerBridge;
import io.izzel.arclight.common.bridge.core.server.dedicated.DedicatedServerBridge;
import io.izzel.arclight.common.bridge.core.world.level.GameRules_ValueBridge;
import io.izzel.arclight.common.bridge.core.world.level.storage.LevelStorageSourceBridge;
import io.izzel.arclight.common.bridge.core.world.level.storage.PrimaryLevelDataBridge;
import io.izzel.arclight.i18n.ArclightConfig;
import jline.console.ConsoleReader;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.NbtException;
import net.minecraft.nbt.ReportedNbtException;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
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
import net.minecraft.world.entity.npc.WanderingTraderSpawner;
import net.minecraft.world.level.CustomSpawner;
import net.minecraft.world.level.GameRules;
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
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.craftbukkit.v.CraftServer;
import org.bukkit.craftbukkit.v.CraftWorld;
import org.bukkit.craftbukkit.v.command.CraftCommandMap;
import org.bukkit.craftbukkit.v.entity.CraftPlayer;
import org.bukkit.craftbukkit.v.generator.CraftWorldInfo;
import org.bukkit.craftbukkit.v.scheduler.CraftScheduler;
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

    // @formatter:off
    @Shadow @Final private CraftCommandMap commandMap;
    @Shadow @Final private SimplePluginManager pluginManager;
    @Shadow @Final protected DedicatedServer console;
    @Shadow @Final @Mutable private String serverName;
    @Shadow @Final @Mutable protected DedicatedPlayerList playerList;
    @Shadow @Final @Mutable private List<CraftPlayer> playerView;
    @Shadow @Final private Map<String, World> worlds;
    @Shadow public int reloadCount;
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

    @Shadow
    public abstract File getWorldContainer();

    @Shadow
    public abstract World getWorld(String name);

    @Shadow
    public abstract GameMode getDefaultGameMode();

    @Shadow
    public abstract DedicatedServer getServer();

    @Inject(method = "<init>", at = @At("RETURN"))
    public void arclight$setBrand(DedicatedServer console, PlayerList playerList, CallbackInfo ci) {
        this.serverName = "Arclight";
    }

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

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite(remap = false)
    public ConsoleReader getReader() {
        return null;
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
        this.worlds.remove(world.bridge$getWorld().getName().toLowerCase(Locale.ROOT));
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
                        final var location = ResourceLocation.tryBuild("bukkit", name);
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

            Dynamic<?> dynamic;
            if (worldSession.hasWorldData()) {
                LevelSummary worldinfo;
                try {
                    dynamic = worldSession.getDataTag();
                    worldinfo = worldSession.getSummary(dynamic);
                } catch (ReportedNbtException | IOException | NbtException ioexception) {
                    LevelStorageSource.LevelDirectory convertable_b = worldSession.getLevelDirectory();
                    MinecraftServer.LOGGER.warn("Failed to load world data from {}", convertable_b.dataFile(), ioexception);
                    MinecraftServer.LOGGER.info("Attempting to use fallback");

                    try {
                        dynamic = worldSession.getDataTagFallback();
                        worldinfo = worldSession.getSummary(dynamic);
                    } catch (ReportedNbtException | IOException | NbtException ioexception1) {
                        MinecraftServer.LOGGER.error("Failed to load world data from {}", convertable_b.oldDataFile(), ioexception1);
                        MinecraftServer.LOGGER.error("Failed to load world data from {} and {}. World files may be corrupted. Shutting down.", convertable_b.dataFile(), convertable_b.oldDataFile());
                        return null;
                    }

                    worldSession.restoreLevelDataFromOld();
                }

                if (worldinfo.requiresManualConversion()) {
                    MinecraftServer.LOGGER.info("This world must be opened in an older version (like 1.6.4) to be safely converted");
                    return null;
                }

                if (!worldinfo.isCompatible()) {
                    MinecraftServer.LOGGER.info("This world was created by an incompatible version.");
                    return null;
                }
            } else {
                dynamic = null;
            }

            boolean hardcore = creator.hardcore();
            WorldLoader.DataLoadContext context = ((DedicatedServerBridge) this.console).arclight$dataLoadContext();
            RegistryAccess.Frozen datapackDimensions = context.datapackDimensions();
            Registry<LevelStem> datapackStems = datapackDimensions.registryOrThrow(Registries.LEVEL_STEM);
            RegistryAccess.Frozen dimensions;
            PrimaryLevelData levelData;
            if (dynamic != null) {
                LevelDataAndDimensions levelDataAndDimensions = LevelStorageSource.getLevelDataAndDimensions(dynamic, context.dataConfiguration(), datapackStems, context.datapackWorldgen());
                levelData = (PrimaryLevelData)levelDataAndDimensions.worldData();
                // Arclight start: handle base generator
                if (!isCustom) {
                    dimensions = levelDataAndDimensions.dimensions().dimensionsRegistryAccess();
                } else {
                    dimensions = this.console.registries().getLayer(RegistryLayer.DIMENSIONS);
                }
                // Arclight end
            } else {
                WorldOptions options = new WorldOptions(creator.seed(), creator.generateStructures(), false);
                LevelSettings settings = new LevelSettings(name, GameType.byId(this.getDefaultGameMode().getValue()), hardcore, Difficulty.EASY, false, new GameRules(), context.dataConfiguration());
                // Arclight start: handle base generator
                if (isCustom) {
                    DedicatedServerProperties.WorldDimensionData properties = new DedicatedServerProperties.WorldDimensionData(GsonHelper.parse(creator.generatorSettings().isEmpty() ? "{}" : creator.generatorSettings()), creator.type().name().toLowerCase(Locale.ROOT));
                    WorldDimensions worldDimensions = properties.create(context.datapackWorldgen());
                    WorldDimensions.Complete baked = worldDimensions.bake(datapackStems);
                    Lifecycle lifecycle = baked.lifecycle().add(context.datapackWorldgen().allRegistriesLifecycle());
                    levelData = new PrimaryLevelData(settings, options, baked.specialWorldProperty(), lifecycle);
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
                    levelData = new PrimaryLevelData(settings, options, property, template.worldGenSettingsLifecycle());
                    dimensions = this.console.registries().getLayer(RegistryLayer.DIMENSIONS);
                }
                // Arclight end
            }

            Registry<LevelStem> stems = dimensions.registryOrThrow(Registries.LEVEL_STEM);
            LevelStem stem = stems.get(actualDimension);
            if (stem == null) {
                throw new IllegalArgumentException("Unknown level stem: " + actualDimension);
            }
            if (actualDimension != null) {
                ((PrimaryLevelDataBridge) levelData).arclight$offerCustomDimensions(stems);
            }
            ((PrimaryLevelDataBridge) levelData).arclight$checkName(name);
            levelData.setModdedInfo(this.console.getServerModName(), this.console.getModdedStatus().shouldReportAsModified());

            ((DedicatedServerBridge) this.console).arclight$forceUpgradeIfNeeded(worldSession, dimensions); // Arclight

            long j = BiomeManager.obfuscateSeed(creator.seed());
            List<CustomSpawner> list = ImmutableList.of(new PhantomSpawner(), new PatrolSpawner(), new CatSpawner(), new VillageSiege(), new WanderingTraderSpawner(levelData));
            WorldInfo worldInfo = new CraftWorldInfo(levelData, worldSession, creator.environment(), (DimensionType)stem.type().value());
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
                worldKey = ResourceKey.create(Registries.DIMENSION, ResourceLocation.withDefaultNamespace(name.toLowerCase(Locale.ROOT)));
            }

            if (!creator.keepSpawnInMemory()) {
                ((GameRules_ValueBridge<GameRules.IntegerValue>)levelData.getGameRules().getRule(GameRules.RULE_SPAWN_CHUNK_RADIUS)).arclight$set(0, null);
            }

            this.bridge$offerBiomeProviderCache(name, biomeProvider);
            this.bridge$offerGeneratorCache(name, generator);
            this.bridge$offerEnvironmentCache(name, creator.environment());
            ServerLevel internal = new ServerLevel(this.console, this.console.executor, worldSession, levelData, worldKey, stem, this.getServer().progressListenerFactory.create(levelData.getGameRules().getInt(GameRules.RULE_SPAWN_CHUNK_RADIUS)), levelData.isDebugWorld(), j, (List)(creator.environment() == World.Environment.NORMAL ? list : ImmutableList.of()), true, this.console.overworld().getRandomSequences());
            if (!this.worlds.containsKey(name.toLowerCase(Locale.ROOT))) {
                return null;
            } else {
                ((DedicatedServerBridge) this.console).arclight$prepareAndAddLevel(internal, levelData);
                CraftWorld bukkit = internal.bridge$getWorld();
                this.pluginManager.callEvent(new WorldLoadEvent(bukkit));
                return bukkit;
            }
        }
    }
}
