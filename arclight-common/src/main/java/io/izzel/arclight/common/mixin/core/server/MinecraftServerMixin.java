package io.izzel.arclight.common.mixin.core.server;

import com.google.common.collect.ImmutableList;
import com.mojang.datafixers.DataFixer;
import com.mojang.serialization.DynamicOps;
import io.izzel.arclight.api.ArclightVersion;
import io.izzel.arclight.common.bridge.core.command.ICommandSourceBridge;
import io.izzel.arclight.common.bridge.core.server.MinecraftServerBridge;
import io.izzel.arclight.common.bridge.core.world.WorldBridge;
import io.izzel.arclight.common.mod.ArclightConstants;
import io.izzel.arclight.common.mod.server.BukkitRegistry;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import io.izzel.arclight.common.mod.util.BukkitOptionParser;
import it.unimi.dsi.fastutil.longs.LongIterator;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import net.minecraft.CrashReport;
import net.minecraft.ReportedException;
import net.minecraft.SharedConstants;
import net.minecraft.SystemReport;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.obfuscate.DontObfuscate;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.server.ServerFunctionManager;
import net.minecraft.server.Services;
import net.minecraft.server.TickTask;
import net.minecraft.server.WorldStem;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.server.level.progress.ChunkProgressListenerFactory;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.resources.CloseableResourceManager;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.minecraft.server.players.PlayerList;
import net.minecraft.util.Unit;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.util.profiling.jfr.JvmProfiler;
import net.minecraft.util.thread.ReentrantBlockableEventLoop;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.DataPackConfig;
import net.minecraft.world.level.ForcedChunksSavedData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.level.storage.WorldData;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.internal.BrandingControl;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.RemoteConsoleCommandSender;
import org.bukkit.craftbukkit.v.CraftServer;
import org.bukkit.craftbukkit.v.scoreboard.CraftScoreboardManager;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.event.world.WorldInitEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.plugin.PluginLoadOrder;
import org.slf4j.Logger;
import org.spigotmc.WatchdogThread;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.Proxy;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BooleanSupplier;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin extends ReentrantBlockableEventLoop<TickTask> implements MinecraftServerBridge, ICommandSourceBridge {

    // @formatter:off
    @Shadow private int tickCount;
    @Shadow protected abstract boolean initServer() throws IOException;
    @Shadow protected long nextTickTime;
    @Shadow @Final private ServerStatus status;
    @Shadow @Nullable private String motd;
    @Shadow protected abstract void updateStatusIcon(ServerStatus response);
    @Shadow private volatile boolean running;
    @Shadow private long lastOverloadWarning;
    @Shadow @Final static Logger LOGGER;
    @Shadow public abstract void tickServer(BooleanSupplier hasTimeLeft);
    @Shadow protected abstract boolean haveTime();
    @Shadow private boolean mayHaveDelayedTasks;
    @Shadow private long delayedTasksMaxNextTickTime;
    @Shadow protected abstract void waitUntilNextTick();
    @Shadow private volatile boolean isReady;
    @Shadow protected abstract void onServerCrash(CrashReport report);
    @Shadow public abstract File getServerDirectory();
    @Shadow private boolean stopped;
    @Shadow public abstract void stopServer();
    @Shadow public abstract void onServerExit();
    @Shadow public abstract Commands getCommands();
    @Shadow private ProfilerFiller profiler;
    @Shadow protected abstract void updateMobSpawningFlags();
    @Shadow public abstract ServerLevel overworld();
    @Shadow @Final public Map<ResourceKey<Level>, ServerLevel> levels;
    @Shadow protected abstract void setupDebugLevel(WorldData p_240778_1_);
    @Shadow protected WorldData worldData;
    @Shadow(remap = false) @Deprecated public abstract void markWorldsDirty();
    @Shadow private static void setInitialSpawn(ServerLevel p_177897_, ServerLevelData p_177898_, boolean p_177899_, boolean p_177900_) { }
    @Shadow public abstract boolean isSpawningMonsters();
    @Shadow public abstract boolean isSpawningAnimals();
    @Shadow protected abstract void startMetricsRecordingTick();
    @Shadow protected abstract void endMetricsRecordingTick();
    @Shadow public abstract SystemReport fillSystemReport(SystemReport p_177936_);
    @Shadow private float averageTickTime;
    @Shadow @Final private PackRepository packRepository;
    @Shadow public abstract boolean isDedicatedServer();
    @Shadow public abstract int getFunctionCompilationLevel();
    @Shadow @Final public Executor executor;
    @Shadow public abstract RegistryAccess.Frozen registryAccess();
    @Shadow public MinecraftServer.ReloadableResources resources;
    @Shadow private static DataPackConfig getSelectedPacks(PackRepository p_129818_) { return null; }
    @Shadow public abstract PlayerList getPlayerList();
    @Shadow @Final private ServerFunctionManager functionManager;
    @Shadow public abstract boolean previewsChat();
    @Shadow public abstract boolean enforceSecureProfile();
    @Shadow @Final protected Services services;
    @Shadow private static CrashReport constructOrExtractCrashReport(Throwable p_206569_) { return null; }
    @Shadow @Final private StructureTemplateManager structureTemplateManager;
    // @formatter:on

    public MinecraftServerMixin(String name) {
        super(name);
    }

    public DataPackConfig datapackconfiguration;
    public DynamicOps<Tag> registryreadops;
    private boolean forceTicks;
    public CraftServer server;
    public OptionSet options;
    public ConsoleCommandSender console;
    public RemoteConsoleCommandSender remoteConsole;
    public java.util.Queue<Runnable> processQueue = new java.util.concurrent.ConcurrentLinkedQueue<>();
    public int autosavePeriod;
    public Commands vanillaCommandDispatcher;
    private boolean hasStopped = false;
    private final Object stopLock = new Object();

    private static final int TPS = 20;
    private static final int TICK_TIME = 1000000000 / TPS;
    private static final int SAMPLE_INTERVAL = 100;
    private static int currentTick = (int) (System.currentTimeMillis() / 50);
    public final double[] recentTps = new double[3];

    public boolean hasStopped() {
        synchronized (stopLock) {
            return hasStopped;
        }
    }

    @Override
    public boolean bridge$hasStopped() {
        return this.hasStopped();
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    public void arclight$loadOptions(Thread p_236723_, LevelStorageSource.LevelStorageAccess p_236724_, PackRepository p_236725_, WorldStem worldStem, Proxy p_236727_, DataFixer p_236728_, Services p_236729_, ChunkProgressListenerFactory p_236730_, CallbackInfo ci) {
        String[] arguments = ManagementFactory.getRuntimeMXBean().getInputArguments().toArray(new String[0]);
        OptionParser parser = new BukkitOptionParser();
        try {
            options = parser.parse(arguments);
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.datapackconfiguration = ArclightCaptures.getDatapackConfig();
        this.registryreadops = RegistryOps.create(NbtOps.INSTANCE, worldStem.registryAccess());
        this.vanillaCommandDispatcher = worldStem.dataPackResources().getCommands();
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    protected void runServer() {
        try {
            if (!this.initServer()) {
                throw new IllegalStateException("Failed to initialize server");
            }
            ServerLifecycleHooks.handleServerStarted((MinecraftServer) (Object) this);
            this.nextTickTime = Util.getMillis();
            this.status.setDescription(Component.literal(this.motd));
            this.status.setVersion(new ServerStatus.Version(SharedConstants.getCurrentVersion().getName(), SharedConstants.getCurrentVersion().getProtocolVersion()));
            this.status.setPreviewsChat(this.previewsChat());
            this.status.setEnforcesSecureChat(this.enforceSecureProfile());
            this.updateStatusIcon(this.status);

            Arrays.fill(recentTps, 20);
            long curTime, tickSection = Util.getMillis(), tickCount = 1;

            while (this.running) {
                long i = (curTime = Util.getMillis()) - this.nextTickTime;
                if (i > 2000L && this.nextTickTime - this.lastOverloadWarning >= 15000L) {
                    long j = i / 50L;

                    if (server.getWarnOnOverload()) {
                        LOGGER.warn("Can't keep up! Is the server overloaded? Running {}ms or {} ticks behind", i, j);
                    }

                    this.nextTickTime += j * 50L;
                    this.lastOverloadWarning = this.nextTickTime;
                }

                if (tickCount++ % SAMPLE_INTERVAL == 0) {
                    double currentTps = 1E3 / (curTime - tickSection) * SAMPLE_INTERVAL;
                    recentTps[0] = calcTps(recentTps[0], 0.92, currentTps); // 1/exp(5sec/1min)
                    recentTps[1] = calcTps(recentTps[1], 0.9835, currentTps); // 1/exp(5sec/5min)
                    recentTps[2] = calcTps(recentTps[2], 0.9945, currentTps); // 1/exp(5sec/15min)
                    tickSection = curTime;
                }

                currentTick = (int) (System.currentTimeMillis() / 50);

                this.nextTickTime += 50L;
                this.startMetricsRecordingTick();
                this.profiler.push("tick");
                this.tickServer(this::haveTime);
                this.profiler.popPush("nextTickWait");
                this.mayHaveDelayedTasks = true;
                this.delayedTasksMaxNextTickTime = Math.max(Util.getMillis() + 50L, this.nextTickTime);
                this.waitUntilNextTick();
                this.profiler.pop();
                this.endMetricsRecordingTick();
                this.isReady = true;
                JvmProfiler.INSTANCE.onServerTick(this.averageTickTime);
            }
            ServerLifecycleHooks.handleServerStopping((MinecraftServer) (Object) this);
            ServerLifecycleHooks.expectServerStopped(); // has to come before finalTick to avoid race conditions
        } catch (Throwable throwable1) {
            LOGGER.error("Encountered an unexpected exception", throwable1);
            CrashReport crashreport = constructOrExtractCrashReport(throwable1);
            this.fillSystemReport(crashreport.getSystemReport());
            File file1 = new File(new File(this.getServerDirectory(), "crash-reports"), "crash-" + Util.getFilenameFormattedDateTime() + "-server.txt");
            if (crashreport.saveToFile(file1)) {
                LOGGER.error("This crash report has been saved to: {}", file1.getAbsolutePath());
            } else {
                LOGGER.error("We were unable to save this crash report to disk.");
            }

            net.minecraftforge.server.ServerLifecycleHooks.expectServerStopped(); // Forge: Has to come before MinecraftServer#onServerCrash to avoid race conditions
            this.onServerCrash(crashreport);
        } finally {
            try {
                this.stopped = true;
                this.stopServer();
            } catch (Throwable throwable) {
                LOGGER.error("Exception stopping the server", throwable);
            } finally {
                if (this.services.profileCache() != null) {
                    this.services.profileCache().clearExecutor();
                }
                WatchdogThread.doStop();
                ServerLifecycleHooks.handleServerStopped((MinecraftServer) (Object) this);
                this.onServerExit();
            }
        }
    }

    private static double calcTps(double avg, double exp, double tps) {
        return (avg * exp) + (tps * (1 - exp));
    }

    @Inject(method = "stopServer", cancellable = true, at = @At("HEAD"))
    public void arclight$setStopped(CallbackInfo ci) {
        synchronized (stopLock) {
            if (hasStopped) {
                ci.cancel();
                return;
            }
            hasStopped = true;
        }
    }

    @Inject(method = "stopServer", at = @At(value = "INVOKE", remap = false, ordinal = 0, shift = At.Shift.AFTER, target = "Lorg/slf4j/Logger;info(Ljava/lang/String;)V"))
    public void arclight$unloadPlugins(CallbackInfo ci) {
        if (this.server != null) {
            this.server.disablePlugins();
        }
    }

    @Inject(method = "createLevels", at = @At("RETURN"))
    public void arclight$enablePlugins(ChunkProgressListener p_240787_1_, CallbackInfo ci) {
        BukkitRegistry.unlockRegistries();
        this.server.enablePlugins(PluginLoadOrder.POSTWORLD);
        BukkitRegistry.lockRegistries();
        this.server.getPluginManager().callEvent(new ServerLoadEvent(ServerLoadEvent.LoadType.STARTUP));
    }

    private void executeModerately() {
        this.runAllTasks();
        this.bridge$drainQueuedTasks();
        java.util.concurrent.locks.LockSupport.parkNanos("executing tasks", 1000L);
    }

    @Override
    public void bridge$drainQueuedTasks() {
        while (!processQueue.isEmpty()) {
            processQueue.remove().run();
        }
    }

    @Inject(method = "haveTime", cancellable = true, at = @At("HEAD"))
    private void arclight$forceAheadOfTime(CallbackInfoReturnable<Boolean> cir) {
        if (this.forceTicks) cir.setReturnValue(true);
    }

    @Inject(method = "createLevels", at = @At(value = "NEW", ordinal = 0, target = "net/minecraft/server/level/ServerLevel"))
    private void arclight$registerEnv(ChunkProgressListener p_240787_1_, CallbackInfo ci) {
        BukkitRegistry.registerEnvironments(this.worldData.worldGenSettings().dimensions());
    }

    @Redirect(method = "createLevels", at = @At(value = "INVOKE", remap = false, target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"))
    private Object arclight$worldInit(Map<Object, Object> map, Object key, Object value) {
        Object ret = map.put(key, value);
        ServerLevel serverWorld = (ServerLevel) value;
        if (((CraftServer) Bukkit.getServer()).scoreboardManager == null) {
            ((CraftServer) Bukkit.getServer()).scoreboardManager = new CraftScoreboardManager((MinecraftServer) (Object) this, serverWorld.getScoreboard());
        }
        if (((WorldBridge) serverWorld).bridge$getGenerator() != null) {
            ((WorldBridge) serverWorld).bridge$getWorld().getPopulators().addAll(
                ((WorldBridge) serverWorld).bridge$getGenerator().getDefaultPopulators(
                    ((WorldBridge) serverWorld).bridge$getWorld()));
        }
        Bukkit.getPluginManager().callEvent(new WorldInitEvent(((WorldBridge) serverWorld).bridge$getWorld()));
        return ret;
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public void prepareLevels(ChunkProgressListener listener) {
        ServerLevel serverworld = this.overworld();
        this.forceTicks = true;
        LOGGER.info("Preparing start region for dimension {}", serverworld.dimension().location());
        BlockPos blockpos = serverworld.getSharedSpawnPos();
        listener.updateSpawnPos(new ChunkPos(blockpos));
        ServerChunkCache serverchunkprovider = serverworld.getChunkSource();
        serverchunkprovider.getLightEngine().setTaskPerBatch(500);
        this.nextTickTime = Util.getMillis();
        serverchunkprovider.addRegionTicket(TicketType.START, new ChunkPos(blockpos), 11, Unit.INSTANCE);

        while (serverchunkprovider.getTickingGenerated() < 441) {
            this.executeModerately();
        }

        this.executeModerately();

        for (ServerLevel serverWorld : this.levels.values()) {
            if (((WorldBridge) serverWorld).bridge$getWorld().getKeepSpawnInMemory()) {
                ForcedChunksSavedData forcedchunkssavedata = serverWorld.getDataStorage().get(ForcedChunksSavedData::load, "chunks");
                if (forcedchunkssavedata != null) {
                    LongIterator longiterator = forcedchunkssavedata.getChunks().iterator();

                    while (longiterator.hasNext()) {
                        long i = longiterator.nextLong();
                        ChunkPos chunkpos = new ChunkPos(i);
                        serverWorld.getChunkSource().updateChunkForced(chunkpos, true);
                    }
                    net.minecraftforge.common.world.ForgeChunkManager.reinstatePersistentChunks(serverWorld, forcedchunkssavedata);
                }
            }
            Bukkit.getPluginManager().callEvent(new WorldLoadEvent(((WorldBridge) serverWorld).bridge$getWorld()));
        }

        this.executeModerately();
        listener.stop();
        serverchunkprovider.getLightEngine().setTaskPerBatch(5);
        this.updateMobSpawningFlags();
        this.forceTicks = false;
    }

    // bukkit methods
    public void initWorld(ServerLevel serverWorld, ServerLevelData worldInfo, WorldData saveData, WorldGenSettings generatorSettings) {
        boolean flag = generatorSettings.isDebug();
        if (((WorldBridge) serverWorld).bridge$getGenerator() != null) {
            ((WorldBridge) serverWorld).bridge$getWorld().getPopulators().addAll(
                ((WorldBridge) serverWorld).bridge$getGenerator().getDefaultPopulators(
                    ((WorldBridge) serverWorld).bridge$getWorld()));
        }
        WorldBorder worldborder = serverWorld.getWorldBorder();
        worldborder.applySettings(worldInfo.getWorldBorder());
        if (!worldInfo.isInitialized()) {
            try {
                setInitialSpawn(serverWorld, worldInfo, generatorSettings.generateBonusChest(), flag);
                worldInfo.setInitialized(true);
                if (flag) {
                    this.setupDebugLevel(this.worldData);
                }
            } catch (Throwable throwable) {
                CrashReport crashreport = CrashReport.forThrowable(throwable, "Exception initializing level");
                try {
                    serverWorld.fillReportDetails(crashreport);
                } catch (Throwable throwable2) {
                    // empty catch block
                }
                throw new ReportedException(crashreport);
            }
            worldInfo.setInitialized(true);
        }
    }

    // bukkit methods
    public void prepareLevels(ChunkProgressListener listener, ServerLevel serverWorld) {
        this.markWorldsDirty();
        MinecraftForge.EVENT_BUS.post(new LevelEvent.Load(serverWorld));
        if (!((WorldBridge) serverWorld).bridge$getWorld().getKeepSpawnInMemory()) {
            return;
        }
        this.forceTicks = true;
        LOGGER.info("Preparing start region for dimension {}", serverWorld.dimension().location());
        BlockPos blockpos = serverWorld.getSharedSpawnPos();
        listener.updateSpawnPos(new ChunkPos(blockpos));
        ServerChunkCache serverchunkprovider = serverWorld.getChunkSource();
        serverchunkprovider.getLightEngine().setTaskPerBatch(500);
        this.nextTickTime = Util.getMillis();
        serverchunkprovider.addRegionTicket(TicketType.START, new ChunkPos(blockpos), 11, Unit.INSTANCE);

        while (serverchunkprovider.getTickingGenerated() < 441) {
            this.executeModerately();
        }

        this.executeModerately();

        ForcedChunksSavedData forcedchunkssavedata = serverWorld.getDataStorage().get(ForcedChunksSavedData::load, "chunks");
        if (forcedchunkssavedata != null) {
            LongIterator longiterator = forcedchunkssavedata.getChunks().iterator();

            while (longiterator.hasNext()) {
                long i = longiterator.nextLong();
                ChunkPos chunkpos = new ChunkPos(i);
                serverWorld.getChunkSource().updateChunkForced(chunkpos, true);
            }
            net.minecraftforge.common.world.ForgeChunkManager.reinstatePersistentChunks(serverWorld, forcedchunkssavedata);
        }
        this.executeModerately();
        listener.stop();
        serverchunkprovider.getLightEngine().setTaskPerBatch(5);
        // this.updateMobSpawningFlags();
        serverWorld.setSpawnSettings(this.isSpawningMonsters(), this.isSpawningAnimals());
        this.forceTicks = false;
    }

    // bukkit callbacks
    public void addLevel(ServerLevel level) {
        this.levels.put(level.dimension(), level);
        this.markWorldsDirty();
    }

    public void removeLevel(ServerLevel level) {
        MinecraftForge.EVENT_BUS.post(new LevelEvent.Unload(level));
        this.levels.remove(level.dimension());
        this.markWorldsDirty();
    }

    @Inject(method = "tickChildren", at = @At("HEAD"))
    public void arclight$runScheduler(BooleanSupplier hasTimeLeft, CallbackInfo ci) {
        ArclightConstants.currentTick = (int) (System.currentTimeMillis() / 50);
        this.server.getScheduler().mainThreadHeartbeat(this.tickCount);
        this.bridge$drainQueuedTasks();
    }

    @Inject(method = "saveAllChunks", cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;overworld()Lnet/minecraft/server/level/ServerLevel;"))
    private void arclight$skipSave(boolean suppressLog, boolean flush, boolean forced, CallbackInfoReturnable<Boolean> cir, boolean flag) {
        cir.setReturnValue(flag);
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public CompletableFuture<Void> reloadResources(Collection<String> p_129862_) {
        RegistryAccess.Frozen registryaccess$frozen = this.registryAccess();
        CompletableFuture<Void> completablefuture = CompletableFuture.supplyAsync(() -> {
            return p_129862_.stream().map(this.packRepository::getPack).filter(Objects::nonNull).map(Pack::open).collect(ImmutableList.toImmutableList());
        }, this).thenCompose((p_212913_) -> {
            CloseableResourceManager closeableresourcemanager = new MultiPackResourceManager(PackType.SERVER_DATA, p_212913_);
            return ReloadableServerResources.loadResources(closeableresourcemanager, registryaccess$frozen, this.isDedicatedServer() ? Commands.CommandSelection.DEDICATED : Commands.CommandSelection.INTEGRATED, this.getFunctionCompilationLevel(), this.executor, this).whenComplete((p_212907_, p_212908_) -> {
                if (p_212908_ != null) {
                    closeableresourcemanager.close();
                }

            }).thenApply((p_212904_) -> {
                return new MinecraftServer.ReloadableResources(closeableresourcemanager, p_212904_);
            });
        }).thenAcceptAsync((p_212919_) -> {
            this.resources.close();
            this.resources = p_212919_;
            this.server.syncCommands();
            this.packRepository.setSelected(p_129862_);
            this.worldData.setDataPackConfig(getSelectedPacks(this.packRepository));
            this.resources.managers().updateRegistryTags(this.registryAccess());
            this.getPlayerList().saveAll();
            this.getPlayerList().reloadResources();
            this.functionManager.replaceLibrary(this.resources.managers().getFunctionLibrary());
            this.structureTemplateManager.onResourceManagerReload(this.resources.resourceManager());
            this.getPlayerList().getPlayers().forEach(this.getPlayerList()::sendPlayerPermissionLevel); //Forge: Fix newly added/modified commands not being sent to the client when commands reload.
        }, this);
        if (this.isSameThread()) {
            this.managedBlock(completablefuture::isDone);
        }

        return completablefuture;
    }

    /**
     * @author IzzelAliz
     * @reason our branding, no one should fuck this up
     */
    @DontObfuscate
    @Overwrite
    public String getServerModName() {
        return BrandingControl.getServerBranding() + " arclight/" + ArclightVersion.current().getReleaseName();
    }

    @Override
    public void bridge$setAutosavePeriod(int autosavePeriod) {
        this.autosavePeriod = autosavePeriod;
    }

    @Override
    public void bridge$setConsole(ConsoleCommandSender console) {
        this.console = console;
    }

    @Override
    public void bridge$setServer(CraftServer server) {
        this.server = server;
    }

    @Override
    public RemoteConsoleCommandSender bridge$getRemoteConsole() {
        return remoteConsole;
    }

    @Override
    public void bridge$setRemoteConsole(RemoteConsoleCommandSender sender) {
        this.remoteConsole = sender;
    }

    @Override
    public void bridge$queuedProcess(Runnable runnable) {
        processQueue.add(runnable);
    }

    public CommandSender getBukkitSender(CommandSourceStack wrapper) {
        return console;
    }

    @Override
    public CommandSender bridge$getBukkitSender(CommandSourceStack wrapper) {
        return getBukkitSender(wrapper);
    }

    @Override
    public Commands bridge$getVanillaCommands() {
        return this.vanillaCommandDispatcher;
    }

    public boolean isDebugging() {
        return false;
    }

    private static MinecraftServer getServer() {
        return Bukkit.getServer() instanceof CraftServer ? ((CraftServer) Bukkit.getServer()).getServer() : null;
    }
}

