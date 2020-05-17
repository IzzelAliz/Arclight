package io.izzel.arclight.mixin.core.server;

import com.google.common.io.Files;
import com.google.gson.JsonElement;
import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.mojang.datafixers.DataFixer;
import io.izzel.arclight.bridge.command.ICommandSourceBridge;
import io.izzel.arclight.bridge.server.MinecraftServerBridge;
import io.izzel.arclight.bridge.world.WorldBridge;
import io.izzel.arclight.mod.ArclightConstants;
import io.izzel.arclight.mod.server.BukkitRegistry;
import io.izzel.arclight.mod.util.BukkitOptionParser;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.ReportedException;
import net.minecraft.network.ServerStatusResponse;
import net.minecraft.profiler.DebugProfiler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerProfileCache;
import net.minecraft.util.SharedConstants;
import net.minecraft.util.Util;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraft.world.chunk.listener.IChunkStatusListener;
import net.minecraft.world.chunk.listener.IChunkStatusListenerFactory;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.SaveHandler;
import net.minecraft.world.storage.WorldInfo;
import net.minecraftforge.fml.StartupQuery;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import org.apache.logging.log4j.Logger;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.RemoteConsoleCommandSender;
import org.bukkit.craftbukkit.v1_14_R1.CraftServer;
import org.bukkit.craftbukkit.v1_14_R1.scoreboard.CraftScoreboardManager;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.event.world.WorldInitEvent;
import org.bukkit.plugin.PluginLoadOrder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.Proxy;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.function.BooleanSupplier;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin implements MinecraftServerBridge, ICommandSourceBridge {

    // @formatter:off
    @Shadow private int tickCounter;
    @Shadow protected abstract boolean init() throws IOException;
    @Shadow protected long serverTime;
    @Shadow @Final private ServerStatusResponse statusResponse;
    @Shadow @Nullable private String motd;
    @Shadow public abstract void applyServerIconToResponse(ServerStatusResponse response);
    @Shadow private volatile boolean serverRunning;
    @Shadow private long timeOfLastWarning;
    @Shadow @Final private static Logger LOGGER;
    @Shadow private boolean startProfiling;
    @Shadow @Final private DebugProfiler profiler;
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
    // @formatter:on

    public CraftServer server;
    public OptionSet options;
    public ConsoleCommandSender console;
    public RemoteConsoleCommandSender remoteConsole;
    public java.util.Queue<Runnable> processQueue = new java.util.concurrent.ConcurrentLinkedQueue<>();
    public int autosavePeriod;
    public File bukkitDataPackFolder;
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

    @Inject(method = "<init>", at = @At("RETURN"))
    public void arclight$loadOptions(File file, Proxy proxy, DataFixer dataFixerIn, Commands commands, YggdrasilAuthenticationService authenticationService, MinecraftSessionService sessionService, GameProfileRepository profileRepository, PlayerProfileCache playerProfileCache, IChunkStatusListenerFactory listenerFactory, String string, CallbackInfo ci) {
        String[] arguments = ManagementFactory.getRuntimeMXBean().getInputArguments().toArray(new String[0]);
        OptionParser parser = new BukkitOptionParser();
        try {
            options = parser.parse(arguments);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public void run() {
        try {
            if (this.init()) {
                ServerLifecycleHooks.handleServerStarted((MinecraftServer) (Object) this);
                this.serverTime = Util.milliTime();
                this.statusResponse.setServerDescription(new StringTextComponent(this.motd));
                this.statusResponse.setVersion(new ServerStatusResponse.Version(SharedConstants.getVersion().getName(), SharedConstants.getVersion().getProtocolVersion()));
                this.applyServerIconToResponse(this.statusResponse);

                Arrays.fill(recentTps, 0);
                long curTime, tickSection = Util.milliTime(), tickCount = 1;

                while (this.serverRunning) {
                    long i = (curTime = Util.milliTime()) - this.serverTime;
                    if (i > 5000L && this.serverTime - this.timeOfLastWarning >= 30000L) {
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
                    if (this.startProfiling) {
                        this.startProfiling = false;
                        this.profiler.func_219899_d().func_219939_d();
                    }

                    this.profiler.startTick();
                    this.profiler.startSection("tick");
                    this.tick(this::isAheadOfTime);
                    this.profiler.endStartSection("nextTickWait");
                    this.isRunningScheduledTasks = true;
                    this.runTasksUntil = Math.max(Util.milliTime() + 50L, this.serverTime);
                    this.runScheduledTasks();
                    this.profiler.endSection();
                    this.profiler.endTick();
                    this.serverIsRunning = true;
                }
                ServerLifecycleHooks.handleServerStopping((MinecraftServer) (Object) this);
                ServerLifecycleHooks.expectServerStopped(); // has to come before finalTick to avoid race conditions
            } else {
                ServerLifecycleHooks.expectServerStopped(); // has to come before finalTick to avoid race conditions
                this.finalTick((CrashReport) null);
            }
        } catch (StartupQuery.AbortedException e) {
            // ignore silently
            ServerLifecycleHooks.expectServerStopped(); // has to come before finalTick to avoid race conditions
        } catch (Throwable throwable1) {
            LOGGER.error("Encountered an unexpected exception", throwable1);
            CrashReport crashreport;
            if (throwable1 instanceof ReportedException) {
                crashreport = this.addServerInfoToCrashReport(((ReportedException) throwable1).getCrashReport());
            } else {
                crashreport = this.addServerInfoToCrashReport(new CrashReport("Exception in server tick loop", throwable1));
            }

            File file1 = new File(new File(this.getDataDirectory(), "crash-reports"), "crash-" + (new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss")).format(new Date()) + "-server.txt");
            if (crashreport.saveToFile(file1)) {
                LOGGER.error("This crash report has been saved to: {}", (Object) file1.getAbsolutePath());
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

    @Inject(method = "loadAllWorlds", at = @At("RETURN"))
    public void arclight$enablePlugins(String saveName, String worldNameIn, long seed, WorldType type, JsonElement generatorOptions, CallbackInfo ci) {
        BukkitRegistry.unlockRegistries();
        this.server.enablePlugins(PluginLoadOrder.POSTWORLD);
        BukkitRegistry.lockRegistries();
        this.server.getPluginManager().callEvent(new ServerLoadEvent(ServerLoadEvent.LoadType.STARTUP));
    }

    @Inject(method = "loadWorlds", locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/server/MinecraftServer;func_213204_a(Lnet/minecraft/world/storage/DimensionSavedDataManager;)V"))
    private void arclight$worldInit(SaveHandler p_213194_1_, WorldInfo info, WorldSettings p_213194_3_, IChunkStatusListener p_213194_4_, CallbackInfo ci, ServerWorld serverWorld) {
        if (((CraftServer) Bukkit.getServer()).scoreboardManager == null) {
            ((CraftServer) Bukkit.getServer()).scoreboardManager = new CraftScoreboardManager((MinecraftServer) (Object) this, serverWorld.getScoreboard());
        }
        Bukkit.getPluginManager().callEvent(new WorldInitEvent(((WorldBridge) serverWorld).bridge$getWorld()));
        if (((WorldBridge) serverWorld).bridge$getGenerator() != null) {
            ((WorldBridge) serverWorld).bridge$getWorld().getPopulators().addAll(
                ((WorldBridge) serverWorld).bridge$getGenerator().getDefaultPopulators(
                    ((WorldBridge) serverWorld).bridge$getWorld()));
        }
    }

    @Inject(method = "updateTimeLightAndEntities", at = @At("HEAD"))
    public void arclight$runScheduler(BooleanSupplier hasTimeLeft, CallbackInfo ci) {
        ArclightConstants.currentTick = (int) (System.currentTimeMillis() / 50);
        this.server.getScheduler().mainThreadHeartbeat(this.tickCounter);
        while (!processQueue.isEmpty()) {
            processQueue.remove().run();
        }
    }

    @Inject(method = "loadDataPacks(Ljava/io/File;Lnet/minecraft/world/storage/WorldInfo;)V", at = @At("HEAD"))
    private void arclight$bukkitDatapack(File file, WorldInfo p_195560_2_, CallbackInfo ci) {
        this.bukkitDataPackFolder = new File(new File(file, "datapacks"), "bukkit");
        if (!this.bukkitDataPackFolder.exists()) {
            this.bukkitDataPackFolder.mkdirs();
        }
        final File mcMeta = new File(this.bukkitDataPackFolder, "pack.mcmeta");
        try {
            Files.write("{\n    \"pack\": {\n        \"description\": \"Data pack for resources provided by Bukkit plugins\",\n        \"pack_format\": " + SharedConstants.getVersion().getPackVersion() + "\n" + "    }\n" + "}\n", mcMeta, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new RuntimeException("Could not initialize Bukkit datapack", ex);
        }
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

    @Override
    public CommandSender bridge$getBukkitSender(CommandSource wrapper) {
        return console;
    }

    private static MinecraftServer getServer() {
        return Bukkit.getServer() instanceof CraftServer ? ((CraftServer) Bukkit.getServer()).getServer() : null;
    }
}

