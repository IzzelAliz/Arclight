package io.izzel.arclight.common.mixin.core.server;

import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.datafixers.DataFixer;
import io.izzel.arclight.common.bridge.command.ICommandSourceBridge;
import io.izzel.arclight.common.bridge.server.MinecraftServerBridge;
import io.izzel.arclight.common.bridge.world.WorldBridge;
import io.izzel.arclight.common.mod.ArclightConstants;
import io.izzel.arclight.common.mod.server.BukkitRegistry;
import io.izzel.arclight.common.mod.util.ArclightCaptures;
import io.izzel.arclight.common.mod.util.BukkitOptionParser;
import it.unimi.dsi.fastutil.longs.LongIterator;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.ReportedException;
import net.minecraft.network.ServerStatusResponse;
import net.minecraft.profiler.IProfiler;
import net.minecraft.profiler.LongTickDetector;
import net.minecraft.resources.DataPackRegistries;
import net.minecraft.resources.ResourcePackList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerProfileCache;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.SharedConstants;
import net.minecraft.util.Unit;
import net.minecraft.util.Util;
import net.minecraft.util.concurrent.RecursiveEventLoop;
import net.minecraft.util.concurrent.TickDelayedTask;
import net.minecraft.util.datafix.codec.DatapackCodec;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.registry.DynamicRegistries;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.ForcedChunksSaveData;
import net.minecraft.world.World;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.chunk.listener.IChunkStatusListener;
import net.minecraft.world.chunk.listener.IChunkStatusListenerFactory;
import net.minecraft.world.gen.settings.DimensionGeneratorSettings;
import net.minecraft.world.server.ServerChunkProvider;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.server.TicketType;
import net.minecraft.world.storage.IServerConfiguration;
import net.minecraft.world.storage.IServerWorldInfo;
import net.minecraft.world.storage.SaveFormat;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.BrandingControl;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import org.apache.logging.log4j.Logger;
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
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.function.BooleanSupplier;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin extends RecursiveEventLoop<TickDelayedTask> implements MinecraftServerBridge, ICommandSourceBridge {

    // @formatter:off
    @Shadow private int tickCounter;
    @Shadow protected abstract boolean init() throws IOException;
    @Shadow protected long serverTime;
    @Shadow @Final private ServerStatusResponse statusResponse;
    @Shadow @Nullable private String motd;
    @Shadow protected abstract void applyServerIconToResponse(ServerStatusResponse response);
    @Shadow private volatile boolean serverRunning;
    @Shadow private long timeOfLastWarning;
    @Shadow @Final private static Logger LOGGER;
    @Shadow private boolean startProfiling;
    @Shadow protected abstract void tick(BooleanSupplier hasTimeLeft);
    @Shadow protected abstract boolean isAheadOfTime();
    @Shadow private boolean isRunningScheduledTasks;
    @Shadow private long runTasksUntil;
    @Shadow protected abstract void runScheduledTasks();
    @Shadow private volatile boolean serverIsRunning;
    @Shadow protected abstract void finalTick(CrashReport report);
    @Shadow public abstract CrashReport addServerInfoToCrashReport(CrashReport report);
    @Shadow public abstract File getDataDirectory();
    @Shadow private boolean serverStopped;
    @Shadow protected abstract void stopServer();
    @Shadow protected abstract void systemExitNow();
    @Shadow public abstract Commands getCommandManager();
    @Shadow protected abstract void func_240773_a_(@org.jetbrains.annotations.Nullable LongTickDetector p_240773_1_);
    @Shadow private IProfiler profiler;
    @Shadow protected abstract void func_240795_b_(@org.jetbrains.annotations.Nullable LongTickDetector p_240795_1_);
    @Shadow protected abstract void func_240794_aZ_();
    @Shadow public abstract ServerWorld func_241755_D_();
    @Shadow @Final public Map<RegistryKey<World>, ServerWorld> worlds;
    @Shadow protected abstract void func_240778_a_(IServerConfiguration p_240778_1_);
    @Shadow protected IServerConfiguration serverConfig;
    @Shadow private static void func_240786_a_(ServerWorld p_240786_0_, IServerWorldInfo p_240786_1_, boolean hasBonusChest, boolean p_240786_3_, boolean p_240786_4_) { }
    @Shadow @Deprecated public abstract void markWorldsDirty();
    // @formatter:on

    public MinecraftServerMixin(String name) {
        super(name);
    }

    public DatapackCodec datapackconfiguration;
    private boolean forceTicks;
    public CraftServer server;
    public OptionSet options;
    public ConsoleCommandSender console;
    public RemoteConsoleCommandSender remoteConsole;
    public java.util.Queue<Runnable> processQueue = new java.util.concurrent.ConcurrentLinkedQueue<>();
    public int autosavePeriod;
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
    public void arclight$loadOptions(Thread serverThread, DynamicRegistries.Impl p_i232576_2_, SaveFormat.LevelSave anvilConverterForAnvilFile, IServerConfiguration p_i232576_4_, ResourcePackList dataPacks, Proxy serverProxy, DataFixer dataFixer, DataPackRegistries dataRegistries, MinecraftSessionService sessionService, GameProfileRepository profileRepo, PlayerProfileCache profileCache, IChunkStatusListenerFactory chunkStatusListenerFactory, CallbackInfo ci) {
        String[] arguments = ManagementFactory.getRuntimeMXBean().getInputArguments().toArray(new String[0]);
        OptionParser parser = new BukkitOptionParser();
        try {
            options = parser.parse(arguments);
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.datapackconfiguration = ArclightCaptures.getDatapackConfig();
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    protected void func_240802_v_() {
        try {
            if (this.init()) {
                ServerLifecycleHooks.handleServerStarted((MinecraftServer) (Object) this);
                this.serverTime = Util.milliTime();
                this.statusResponse.setServerDescription(new StringTextComponent(this.motd));
                this.statusResponse.setVersion(new ServerStatusResponse.Version(SharedConstants.getVersion().getName(), SharedConstants.getVersion().getProtocolVersion()));
                this.applyServerIconToResponse(this.statusResponse);

                Arrays.fill(recentTps, 20);
                long curTime, tickSection = Util.milliTime(), tickCount = 1;

                while (this.serverRunning) {
                    long i = (curTime = Util.milliTime()) - this.serverTime;
                    if (i > 2000L && this.serverTime - this.timeOfLastWarning >= 15000L) {
                        long j = i / 50L;

                        if (server.getWarnOnOverload()) {
                            LOGGER.warn("Can't keep up! Is the server overloaded? Running {}ms or {} ticks behind", i, j);
                        }

                        this.serverTime += j * 50L;
                        this.timeOfLastWarning = this.serverTime;
                    }

                    if (tickCount++ % SAMPLE_INTERVAL == 0) {
                        double currentTps = 1E3 / (curTime - tickSection) * SAMPLE_INTERVAL;
                        recentTps[0] = calcTps(recentTps[0], 0.92, currentTps); // 1/exp(5sec/1min)
                        recentTps[1] = calcTps(recentTps[1], 0.9835, currentTps); // 1/exp(5sec/5min)
                        recentTps[2] = calcTps(recentTps[2], 0.9945, currentTps); // 1/exp(5sec/15min)
                        tickSection = curTime;
                    }

                    currentTick = (int) (System.currentTimeMillis() / 50);

                    this.serverTime += 50L;
                    LongTickDetector longtickdetector = LongTickDetector.func_233524_a_("Server");
                    this.func_240773_a_(longtickdetector);
                    this.profiler.startTick();
                    this.profiler.startSection("tick");
                    this.tick(this::isAheadOfTime);
                    this.profiler.endStartSection("nextTickWait");
                    this.isRunningScheduledTasks = true;
                    this.runTasksUntil = Math.max(Util.milliTime() + 50L, this.serverTime);
                    this.runScheduledTasks();
                    this.profiler.endSection();
                    this.profiler.endTick();
                    this.func_240795_b_(longtickdetector);
                    this.serverIsRunning = true;
                }
                ServerLifecycleHooks.handleServerStopping((MinecraftServer) (Object) this);
                ServerLifecycleHooks.expectServerStopped(); // has to come before finalTick to avoid race conditions
            } else {
                ServerLifecycleHooks.expectServerStopped(); // has to come before finalTick to avoid race conditions
                this.finalTick(null);
            }
        } catch (Throwable throwable1) {
            LOGGER.error("Encountered an unexpected exception", throwable1);

            if (throwable1.getCause() != null) {
                LOGGER.error("\tCause of unexpected exception was", throwable1.getCause());
            }

            CrashReport crashreport;
            if (throwable1 instanceof ReportedException) {
                crashreport = this.addServerInfoToCrashReport(((ReportedException) throwable1).getCrashReport());
            } else {
                crashreport = this.addServerInfoToCrashReport(new CrashReport("Exception in server tick loop", throwable1));
            }

            File file1 = new File(new File(this.getDataDirectory(), "crash-reports"), "crash-" + (new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss")).format(new Date()) + "-server.txt");
            if (crashreport.saveToFile(file1)) {
                LOGGER.error("This crash report has been saved to: {}", file1.getAbsolutePath());
            } else {
                LOGGER.error("We were unable to save this crash report to disk.");
            }

            ServerLifecycleHooks.expectServerStopped(); // has to come before finalTick to avoid race conditions
            this.finalTick(crashreport);
        } finally {
            try {
                this.serverStopped = true;
                this.stopServer();
            } catch (Throwable throwable) {
                LOGGER.error("Exception stopping the server", throwable);
            } finally {
                WatchdogThread.doStop();
                ServerLifecycleHooks.handleServerStopped((MinecraftServer) (Object) this);
                this.systemExitNow();
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

    @Inject(method = "stopServer", at = @At(value = "INVOKE", remap = false, ordinal = 0, shift = At.Shift.AFTER, target = "Lorg/apache/logging/log4j/Logger;info(Ljava/lang/String;)V"))
    public void arclight$unloadPlugins(CallbackInfo ci) {
        if (this.server != null) {
            this.server.disablePlugins();
        }
    }

    @Inject(method = "func_240787_a_", at = @At("RETURN"))
    public void arclight$enablePlugins(IChunkStatusListener p_240787_1_, CallbackInfo ci) {
        BukkitRegistry.unlockRegistries();
        this.server.enablePlugins(PluginLoadOrder.POSTWORLD);
        BukkitRegistry.lockRegistries();
        this.server.getPluginManager().callEvent(new ServerLoadEvent(ServerLoadEvent.LoadType.STARTUP));
    }

    private void executeModerately() {
        this.drainTasks();
        this.bridge$drainQueuedTasks();
        java.util.concurrent.locks.LockSupport.parkNanos("executing tasks", 1000L);
    }

    @Override
    public void bridge$drainQueuedTasks() {
        while (!processQueue.isEmpty()) {
            processQueue.remove().run();
        }
    }

    @Inject(method = "isAheadOfTime", cancellable = true, at = @At("HEAD"))
    private void arclight$forceAheadOfTime(CallbackInfoReturnable<Boolean> cir) {
        if (this.forceTicks) cir.setReturnValue(true);
    }

    @Inject(method = "func_240787_a_", at = @At(value = "NEW", ordinal = 0, target = "net/minecraft/world/server/ServerWorld"))
    private void arclight$registerEnv(IChunkStatusListener p_240787_1_, CallbackInfo ci) {
        BukkitRegistry.registerEnvironments();
    }

    @Redirect(method = "func_240787_a_", at = @At(value = "INVOKE", remap = false, target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"))
    private Object arclight$worldInit(Map<Object, Object> map, Object key, Object value) {
        Object ret = map.put(key, value);
        ServerWorld serverWorld = (ServerWorld) value;
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
    public void loadInitialChunks(IChunkStatusListener listener) {
        ServerWorld serverworld = this.func_241755_D_();
        this.forceTicks = true;
        LOGGER.info("Preparing start region for dimension {}", serverworld.getDimensionKey().getLocation());
        BlockPos blockpos = serverworld.getSpawnPoint();
        listener.start(new ChunkPos(blockpos));
        ServerChunkProvider serverchunkprovider = serverworld.getChunkProvider();
        serverchunkprovider.getLightManager().func_215598_a(500);
        this.serverTime = Util.milliTime();
        serverchunkprovider.registerTicket(TicketType.START, new ChunkPos(blockpos), 11, Unit.INSTANCE);

        while (serverchunkprovider.getLoadedChunksCount() != 441) {
            this.executeModerately();
        }

        this.executeModerately();

        for (ServerWorld serverWorld : this.worlds.values()) {
            if (((WorldBridge) serverWorld).bridge$getWorld().getKeepSpawnInMemory()) {
                ForcedChunksSaveData forcedchunkssavedata = serverWorld.getSavedData().get(ForcedChunksSaveData::new, "chunks");
                if (forcedchunkssavedata != null) {
                    LongIterator longiterator = forcedchunkssavedata.getChunks().iterator();

                    while (longiterator.hasNext()) {
                        long i = longiterator.nextLong();
                        ChunkPos chunkpos = new ChunkPos(i);
                        serverWorld.getChunkProvider().forceChunk(chunkpos, true);
                    }
                }
            }
            Bukkit.getPluginManager().callEvent(new WorldLoadEvent(((WorldBridge) serverWorld).bridge$getWorld()));
        }

        this.executeModerately();
        listener.stop();
        serverchunkprovider.getLightManager().func_215598_a(5);
        this.func_240794_aZ_();
        this.forceTicks = false;
    }

    // bukkit methods
    public void initWorld(ServerWorld serverWorld, IServerWorldInfo worldInfo, IServerConfiguration saveData, DimensionGeneratorSettings generatorSettings) {
        boolean flag = generatorSettings.func_236227_h_();
        if (((WorldBridge) serverWorld).bridge$getGenerator() != null) {
            ((WorldBridge) serverWorld).bridge$getWorld().getPopulators().addAll(
                ((WorldBridge) serverWorld).bridge$getGenerator().getDefaultPopulators(
                    ((WorldBridge) serverWorld).bridge$getWorld()));
        }
        WorldBorder worldborder = serverWorld.getWorldBorder();
        worldborder.deserialize(worldInfo.getWorldBorderSerializer());
        if (!worldInfo.isInitialized()) {
            try {
                func_240786_a_(serverWorld, worldInfo, generatorSettings.hasBonusChest(), flag, true);
                worldInfo.setInitialized(true);
                if (flag) {
                    this.func_240778_a_(this.serverConfig);
                }
            } catch (Throwable throwable) {
                CrashReport crashreport = CrashReport.makeCrashReport(throwable, "Exception initializing level");
                try {
                    serverWorld.fillCrashReport(crashreport);
                } catch (Throwable throwable2) {
                    // empty catch block
                }
                throw new ReportedException(crashreport);
            }
            worldInfo.setInitialized(true);
        }
    }

    // bukkit methods
    public void loadSpawn(IChunkStatusListener listener, ServerWorld serverWorld) {
        this.markWorldsDirty();
        MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.world.WorldEvent.Load(serverWorld));
        if (!((WorldBridge) serverWorld).bridge$getWorld().getKeepSpawnInMemory()) {
            return;
        }
        this.forceTicks = true;
        LOGGER.info("Preparing start region for dimension {}", serverWorld.getDimensionKey().getLocation());
        BlockPos blockpos = serverWorld.getSpawnPoint();
        listener.start(new ChunkPos(blockpos));
        ServerChunkProvider serverchunkprovider = serverWorld.getChunkProvider();
        serverchunkprovider.getLightManager().func_215598_a(500);
        this.serverTime = Util.milliTime();
        serverchunkprovider.registerTicket(TicketType.START, new ChunkPos(blockpos), 11, Unit.INSTANCE);

        while (serverchunkprovider.getLoadedChunksCount() != 441) {
            this.executeModerately();
        }

        this.executeModerately();

        ForcedChunksSaveData forcedchunkssavedata = serverWorld.getSavedData().get(ForcedChunksSaveData::new, "chunks");
        if (forcedchunkssavedata != null) {
            LongIterator longiterator = forcedchunkssavedata.getChunks().iterator();

            while (longiterator.hasNext()) {
                long i = longiterator.nextLong();
                ChunkPos chunkpos = new ChunkPos(i);
                serverWorld.getChunkProvider().forceChunk(chunkpos, true);
            }
        }
        this.executeModerately();
        listener.stop();
        serverchunkprovider.getLightManager().func_215598_a(5);
        this.func_240794_aZ_();
        this.forceTicks = false;
    }

    @Inject(method = "updateTimeLightAndEntities", at = @At("HEAD"))
    public void arclight$runScheduler(BooleanSupplier hasTimeLeft, CallbackInfo ci) {
        ArclightConstants.currentTick = (int) (System.currentTimeMillis() / 50);
        this.server.getScheduler().mainThreadHeartbeat(this.tickCounter);
        this.bridge$drainQueuedTasks();
    }

    @Inject(method = "save", cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;func_241755_D_()Lnet/minecraft/world/server/ServerWorld;"))
    private void arclight$skipSave(boolean suppressLog, boolean flush, boolean forced, CallbackInfoReturnable<Boolean> cir, boolean flag) {
        cir.setReturnValue(flag);
    }

    /**
     * @author IzzelAliz
     * @reason our branding, no one should fuck this up
     */
    @Overwrite
    public String getServerModName() {
        return BrandingControl.getServerBranding() + " arclight";
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

    public CommandSender getBukkitSender(CommandSource wrapper) {
        return console;
    }

    @Override
    public CommandSender bridge$getBukkitSender(CommandSource wrapper) {
        return getBukkitSender(wrapper);
    }

    public boolean isDebugging() {
        return false;
    }

    private static MinecraftServer getServer() {
        return Bukkit.getServer() instanceof CraftServer ? ((CraftServer) Bukkit.getServer()).getServer() : null;
    }
}

