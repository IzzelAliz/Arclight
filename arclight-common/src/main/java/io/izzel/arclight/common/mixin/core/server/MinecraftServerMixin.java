package io.izzel.arclight.common.mixin.core.server;

import com.mojang.datafixers.DataFixer;
import io.izzel.arclight.api.ArclightVersion;
import io.izzel.arclight.common.bridge.bukkit.CraftServerBridge;
import io.izzel.arclight.common.bridge.core.command.ICommandSourceBridge;
import io.izzel.arclight.common.bridge.core.server.MinecraftServerBridge;
import io.izzel.arclight.common.bridge.core.world.WorldBridge;
import io.izzel.arclight.common.mod.ArclightConstants;
import io.izzel.arclight.common.mod.mixins.annotation.TransformAccess;
import io.izzel.arclight.common.mod.server.ArclightServer;
import io.izzel.arclight.common.mod.server.BukkitRegistry;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import io.izzel.arclight.common.mod.util.BukkitOptionParser;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import it.unimi.dsi.fastutil.longs.LongIterator;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import net.minecraft.CrashReport;
import net.minecraft.ReportedException;
import net.minecraft.SystemReport;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.RegistryLayer;
import net.minecraft.server.ServerFunctionManager;
import net.minecraft.server.ServerTickRateManager;
import net.minecraft.server.Services;
import net.minecraft.server.TickTask;
import net.minecraft.server.WorldLoader;
import net.minecraft.server.WorldStem;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.server.level.progress.ChunkProgressListener;
import net.minecraft.server.level.progress.ChunkProgressListenerFactory;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.players.PlayerList;
import net.minecraft.util.TimeUtil;
import net.minecraft.util.Unit;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.util.profiling.jfr.JvmProfiler;
import net.minecraft.util.thread.ReentrantBlockableEventLoop;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.DataPackConfig;
import net.minecraft.world.level.ForcedChunksSavedData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.level.storage.WorldData;
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
import org.objectweb.asm.Opcodes;
import org.slf4j.Logger;
import org.spigotmc.WatchdogThread;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.Proxy;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.function.BooleanSupplier;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin extends ReentrantBlockableEventLoop<TickTask> implements MinecraftServerBridge, ICommandSourceBridge {

    // @formatter:off
    @Shadow private int tickCount;
    @Shadow protected abstract boolean initServer() throws IOException;
    @Shadow protected long nextTickTimeNanos;
    @Shadow private ServerStatus status;
    @Shadow @Nullable private String motd;
    @Shadow private volatile boolean running;
    @Shadow private long lastOverloadWarningNanos;
    @Shadow @Final static Logger LOGGER;
    @Shadow public abstract void tickServer(BooleanSupplier hasTimeLeft);
    @Shadow protected abstract boolean haveTime();
    @Shadow private boolean mayHaveDelayedTasks;
    @Shadow private long delayedTasksMaxNextTickTimeNanos;
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
    @Shadow private Map<ResourceKey<Level>, ServerLevel> levels;
    @Shadow protected abstract void setupDebugLevel(WorldData p_240778_1_);
    @Shadow protected WorldData worldData;
    @Shadow private static void setInitialSpawn(ServerLevel p_177897_, ServerLevelData p_177898_, boolean p_177899_, boolean p_177900_) { }
    @Shadow public abstract boolean isSpawningMonsters();
    @Shadow public abstract boolean isSpawningAnimals();
    @Shadow protected abstract void startMetricsRecordingTick();
    @Shadow protected abstract void endMetricsRecordingTick();
    @Shadow public abstract SystemReport fillSystemReport(SystemReport p_177936_);
    @Shadow @Final private PackRepository packRepository;
    @Shadow public abstract boolean isDedicatedServer();
    @Shadow public abstract int getFunctionCompilationLevel();
    @Shadow @Final public Executor executor;
    @Shadow public abstract RegistryAccess.Frozen registryAccess();
    @Shadow public MinecraftServer.ReloadableResources resources;
    @Shadow private static DataPackConfig getSelectedPacks(PackRepository p_129818_) { return null; }
    @Shadow public abstract PlayerList getPlayerList();
    @Shadow @Final private ServerFunctionManager functionManager;
    @Shadow public abstract boolean enforceSecureProfile();
    @Shadow @Final protected Services services;
    @Shadow private static CrashReport constructOrExtractCrashReport(Throwable p_206569_) { return null; }
    @Shadow @Final private StructureTemplateManager structureTemplateManager;
    @Shadow private boolean debugCommandProfilerDelayStart;
    @Shadow @Nullable private MinecraftServer.TimeProfiler debugCommandProfiler;
    @Shadow public abstract LayeredRegistryAccess<RegistryLayer> registries();
    @Shadow protected abstract ServerStatus buildServerStatus();
    @Shadow @Nullable private ServerStatus.Favicon statusIcon;
    @Shadow protected abstract Optional<ServerStatus.Favicon> loadStatusIcon();
    @Shadow public abstract boolean isPaused();
    @Shadow @Final private ServerTickRateManager tickRateManager;
    @Shadow @Final private static long OVERLOADED_THRESHOLD_NANOS;
    @Shadow @Final private static long OVERLOADED_WARNING_INTERVAL_NANOS;
    @Shadow private float smoothedTickTimeMillis;
    @Shadow public abstract Iterable<ServerLevel> getAllLevels();
    // @formatter:on

    public MinecraftServerMixin(String name) {
        super(name);
    }

    public WorldLoader.DataLoadContext worldLoader;
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
    @TransformAccess(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC)
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
        this.vanillaCommandDispatcher = worldStem.dataPackResources().getCommands();
        this.worldLoader = ArclightCaptures.getDataLoadContext();
        ArclightServer.setMinecraftServer((MinecraftServer) (Object) this);
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
            this.bridge$platform$serverStarted();
            this.nextTickTimeNanos = Util.getNanos();
            this.statusIcon = this.loadStatusIcon().orElse(null);
            this.status = this.buildServerStatus();

            Arrays.fill(recentTps, 20);
            long tickSection = Util.getMillis(), tickCount = 1;

            while (this.running) {
                long i;
                if (!this.isPaused() && this.tickRateManager.isSprinting() && this.tickRateManager.checkShouldSprintThisTick()) {
                    i = 0L;
                    this.nextTickTimeNanos = Util.getNanos();
                    this.lastOverloadWarningNanos = this.nextTickTimeNanos;
                } else {
                    i = this.tickRateManager.nanosecondsPerTick();
                    long j = Util.getNanos() - this.nextTickTimeNanos;

                    if (j > OVERLOADED_THRESHOLD_NANOS + 20L * i && this.nextTickTimeNanos - this.lastOverloadWarningNanos >= OVERLOADED_WARNING_INTERVAL_NANOS + 100L * i) {
                        long k = j / i;

                        if (server.getWarnOnOverload()) // CraftBukkit
                            MinecraftServer.LOGGER.warn("Can't keep up! Is the server overloaded? Running {}ms or {} ticks behind", j / TimeUtil.NANOSECONDS_PER_MILLISECOND, k);
                        this.nextTickTimeNanos += k * i;
                        this.lastOverloadWarningNanos = this.nextTickTimeNanos;
                    }
                }

                if (tickCount++ % SAMPLE_INTERVAL == 0) {
                    long curTime = Util.getMillis();
                    double currentTps = 1E3 / (curTime - tickSection) * SAMPLE_INTERVAL;
                    recentTps[0] = calcTps(recentTps[0], 0.92, currentTps); // 1/exp(5sec/1min)
                    recentTps[1] = calcTps(recentTps[1], 0.9835, currentTps); // 1/exp(5sec/5min)
                    recentTps[2] = calcTps(recentTps[2], 0.9945, currentTps); // 1/exp(5sec/15min)
                    tickSection = curTime;
                }

                boolean flag = i == 0L;

                currentTick = (int) (System.currentTimeMillis() / 50);

                if (this.debugCommandProfilerDelayStart) {
                    this.debugCommandProfilerDelayStart = false;
                    this.debugCommandProfiler = new MinecraftServer.TimeProfiler(Util.getNanos(), this.tickCount);
                }

                this.nextTickTimeNanos += i;
                this.startMetricsRecordingTick();
                this.profiler.push("tick");
                this.tickServer(flag ? () -> false : this::haveTime);
                this.profiler.popPush("nextTickWait");
                this.mayHaveDelayedTasks = true;
                this.delayedTasksMaxNextTickTimeNanos = Math.max(Util.getNanos() + i, this.nextTickTimeNanos);
                this.waitUntilNextTick();
                if (flag) {
                    this.tickRateManager.endTickWork();
                }
                this.profiler.pop();
                this.endMetricsRecordingTick();
                this.isReady = true;
                JvmProfiler.INSTANCE.onServerTick(this.smoothedTickTimeMillis);
            }
            this.bridge$platform$serverStopping();
            this.bridge$forge$expectServerStopped(); // has to come before finalTick to avoid race conditions
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

            this.bridge$forge$expectServerStopped(); // Forge: Has to come before MinecraftServer#onServerCrash to avoid race conditions
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
                this.bridge$platform$serverStopped();
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
        this.bridge$forge$unlockRegistries();
        this.server.enablePlugins(PluginLoadOrder.POSTWORLD);
        this.bridge$forge$lockRegistries();
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

    @Inject(method = "createLevels", at = @At(value = "NEW", ordinal = 0, target = "(Lnet/minecraft/server/MinecraftServer;Ljava/util/concurrent/Executor;Lnet/minecraft/world/level/storage/LevelStorageSource$LevelStorageAccess;Lnet/minecraft/world/level/storage/ServerLevelData;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/world/level/dimension/LevelStem;Lnet/minecraft/server/level/progress/ChunkProgressListener;ZJLjava/util/List;ZLnet/minecraft/world/RandomSequences;)Lnet/minecraft/server/level/ServerLevel;"))
    private void arclight$registerEnv(ChunkProgressListener p_240787_1_, CallbackInfo ci) {
        BukkitRegistry.registerEnvironments(this.registryAccess().registryOrThrow(Registries.LEVEL_STEM));
    }

    @Decorate(method = "createLevels", at = @At(value = "INVOKE", remap = false, target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"))
    private Object arclight$worldInit(Map<Object, Object> instance, Object k, Object v, ChunkProgressListener chunkProgressListener) throws Throwable {
        var serverWorld = (ServerLevel) v;
        if (serverWorld != null) {
            if (((CraftServer) Bukkit.getServer()).scoreboardManager == null) {
                ((CraftServer) Bukkit.getServer()).scoreboardManager = new CraftScoreboardManager((MinecraftServer) (Object) this, serverWorld.getScoreboard());
            }
            if (((WorldBridge) serverWorld).bridge$getGenerator() != null) {
                serverWorld.bridge$getWorld().getPopulators().addAll(
                    ((WorldBridge) serverWorld).bridge$getGenerator().getDefaultPopulators(
                        serverWorld.bridge$getWorld()));
            }
            Bukkit.getPluginManager().callEvent(new WorldInitEvent(serverWorld.bridge$getWorld()));
        }
        return DecorationOps.callsite().invoke(instance, k, v);
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public final void prepareLevels(ChunkProgressListener listener) {
        ServerLevel serverworld = this.overworld();
        this.forceTicks = true;
        LOGGER.info("Preparing start region for dimension {}", serverworld.dimension().location());
        BlockPos blockpos = serverworld.getSharedSpawnPos();
        listener.updateSpawnPos(new ChunkPos(blockpos));
        ServerChunkCache serverchunkprovider = serverworld.getChunkSource();
        this.nextTickTimeNanos = Util.getNanos();
        serverchunkprovider.addRegionTicket(TicketType.START, new ChunkPos(blockpos), 11, Unit.INSTANCE);

        while (serverchunkprovider.getTickingGenerated() < 441) {
            this.executeModerately();
        }

        this.executeModerately();

        for (ServerLevel serverWorld : this.levels.values()) {
            if (serverWorld.bridge$getWorld().getKeepSpawnInMemory()) {
                ForcedChunksSavedData forcedchunkssavedata = serverWorld.getDataStorage().get(ForcedChunksSavedData.factory(), "chunks");
                if (forcedchunkssavedata != null) {
                    LongIterator longiterator = forcedchunkssavedata.getChunks().iterator();

                    while (longiterator.hasNext()) {
                        long i = longiterator.nextLong();
                        ChunkPos chunkpos = new ChunkPos(i);
                        serverWorld.getChunkSource().updateChunkForced(chunkpos, true);
                    }
                    this.bridge$forge$reinstatePersistentChunks(serverWorld, forcedchunkssavedata);
                }
            }
            Bukkit.getPluginManager().callEvent(new WorldLoadEvent(serverWorld.bridge$getWorld()));
        }

        this.executeModerately();
        listener.stop();
        this.updateMobSpawningFlags();
        this.forceTicks = false;
    }

    // bukkit methods
    public void initWorld(ServerLevel serverWorld, ServerLevelData worldInfo, WorldData saveData, WorldOptions worldOptions) {
        boolean flag = saveData.isDebugWorld();
        if (((WorldBridge) serverWorld).bridge$getGenerator() != null) {
            serverWorld.bridge$getWorld().getPopulators().addAll(
                ((WorldBridge) serverWorld).bridge$getGenerator().getDefaultPopulators(
                    serverWorld.bridge$getWorld()));
        }
        WorldBorder worldborder = serverWorld.getWorldBorder();
        worldborder.applySettings(worldInfo.getWorldBorder());
        if (!worldInfo.isInitialized()) {
            try {
                setInitialSpawn(serverWorld, worldInfo, worldOptions.generateBonusChest(), flag);
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
        this.bridge$forge$markLevelsDirty();
        this.bridge$platform$loadLevel(serverWorld);
        if (!serverWorld.bridge$getWorld().getKeepSpawnInMemory()) {
            return;
        }
        this.forceTicks = true;
        LOGGER.info("Preparing start region for dimension {}", serverWorld.dimension().location());
        BlockPos blockpos = serverWorld.getSharedSpawnPos();
        listener.updateSpawnPos(new ChunkPos(blockpos));
        ServerChunkCache serverchunkprovider = serverWorld.getChunkSource();
        this.nextTickTimeNanos = Util.getNanos();
        serverchunkprovider.addRegionTicket(TicketType.START, new ChunkPos(blockpos), 11, Unit.INSTANCE);

        while (serverchunkprovider.getTickingGenerated() < 441) {
            this.executeModerately();
        }

        this.executeModerately();

        ForcedChunksSavedData forcedchunkssavedata = serverWorld.getDataStorage().get(ForcedChunksSavedData.factory(), "chunks");
        if (forcedchunkssavedata != null) {
            LongIterator longiterator = forcedchunkssavedata.getChunks().iterator();

            while (longiterator.hasNext()) {
                long i = longiterator.nextLong();
                ChunkPos chunkpos = new ChunkPos(i);
                serverWorld.getChunkSource().updateChunkForced(chunkpos, true);
            }
            this.bridge$forge$reinstatePersistentChunks(serverWorld, forcedchunkssavedata);
        }
        this.executeModerately();
        listener.stop();
        // this.updateMobSpawningFlags();
        serverWorld.setSpawnSettings(this.isSpawningMonsters(), this.isSpawningAnimals());
        this.forceTicks = false;
    }

    // bukkit callbacks
    public void addLevel(ServerLevel level) {
        this.levels.put(level.dimension(), level);
        this.bridge$forge$markLevelsDirty();
    }

    public void removeLevel(ServerLevel level) {
        this.bridge$platform$unloadLevel(level);
        this.levels.remove(level.dimension());
        this.bridge$forge$markLevelsDirty();
        ((CraftServerBridge) Bukkit.getServer()).bridge$removeWorld(level);
    }

    @Inject(method = "tickChildren", at = @At("HEAD"))
    public void arclight$runScheduler(BooleanSupplier hasTimeLeft, CallbackInfo ci) {
        ArclightConstants.currentTick = (int) (System.currentTimeMillis() / 50);
        this.server.getScheduler().mainThreadHeartbeat(this.tickCount);
        this.bridge$drainQueuedTasks();
    }

    @Inject(method = "stopServer", at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/server/MinecraftServer;saveAllChunks(ZZZ)Z"))
    private void arclight$unloadLevel(CallbackInfo ci) {
        for (var serverLevel : this.getAllLevels()) {
            ((CraftServerBridge) Bukkit.getServer()).bridge$removeWorld(serverLevel);
        }
    }

    @Inject(method = "saveAllChunks", cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;overworld()Lnet/minecraft/server/level/ServerLevel;"))
    private void arclight$skipSave(boolean suppressLog, boolean flush, boolean forced, CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(!this.levels.isEmpty());
    }

    @Inject(method = "desc=/V$/", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/packs/repository/PackRepository;setSelected(Ljava/util/Collection;)V"))
    private void arclight$syncCommand(CallbackInfo ci) {
        this.server.syncCommands();
    }

    @Inject(method = "getServerModName", remap = false, cancellable = true, at = @At("RETURN"))
    private void arclight$brand(CallbackInfoReturnable<String> cir) {
        cir.setReturnValue(cir.getReturnValue() + " arclight/" + ArclightVersion.current().getReleaseName());
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

    @TransformAccess(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC)
    private static MinecraftServer getServer() {
        return Bukkit.getServer() instanceof CraftServer ? ((CraftServer) Bukkit.getServer()).getServer() : null;
    }
}

