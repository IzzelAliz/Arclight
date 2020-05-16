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
import io.izzel.arclight.mod.server.BukkitRegistry;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerProfileCache;
import net.minecraft.util.SharedConstants;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraft.world.chunk.listener.IChunkStatusListener;
import net.minecraft.world.chunk.listener.IChunkStatusListenerFactory;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.SaveHandler;
import net.minecraft.world.storage.WorldInfo;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.RemoteConsoleCommandSender;
import org.bukkit.craftbukkit.v1_14_R1.CraftServer;
import org.bukkit.craftbukkit.v1_14_R1.scoreboard.CraftScoreboardManager;
import org.bukkit.event.server.ServerLoadEvent;
import org.bukkit.event.world.WorldInitEvent;
import org.bukkit.plugin.PluginLoadOrder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import io.izzel.arclight.mod.ArclightConstants;
import io.izzel.arclight.mod.util.BukkitOptionParser;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.function.BooleanSupplier;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin implements MinecraftServerBridge, ICommandSourceBridge {

    // @formatter:off
    @Shadow private int tickCounter;
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

