package io.izzel.arclight.common.mixin.core.server.dedicated;

import io.izzel.arclight.common.bridge.core.server.dedicated.DedicatedServerBridge;
import io.izzel.arclight.common.bridge.core.commands.CommandSourceStackBridge;
import io.izzel.arclight.common.mixin.core.server.MinecraftServerMixin;
import io.izzel.arclight.common.mod.server.ArclightServer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.ConsoleInput;
import net.minecraft.server.WorldLoader;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.PrimaryLevelData;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.CraftServer;
import org.bukkit.event.server.RemoteServerCommandEvent;
import org.bukkit.plugin.PluginLoadOrder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(DedicatedServer.class)
public abstract class DedicatedServerMixin extends MinecraftServerMixin implements DedicatedServerBridge {

    public DedicatedServerMixin(String name) {
        super(name);
    }

    @Inject(method = "initServer", at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/server/dedicated/DedicatedServer;setPlayerList(Lnet/minecraft/server/players/PlayerList;)V"))
    public void arclight$loadPlugins(CallbackInfoReturnable<Boolean> cir) {
        this.bridge$forge$unlockRegistries();
        ((CraftServer) Bukkit.getServer()).loadPlugins();
        ((CraftServer) Bukkit.getServer()).enablePlugins(PluginLoadOrder.STARTUP);
        this.bridge$forge$lockRegistries();
    }

    @Inject(method = "initServer", at = @At("RETURN"))
    private void arclight$tickWatchdogIfLaunch(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ()) {
            arclight$tickSpigotWatchdogInternal();
        }
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public String runCommand(String command) {
        throw new UnsupportedOperationException("Not supported - remote source required.");
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public void handleConsoleInput(String command, CommandSourceStack source) {
        var sender = ((CommandSourceStackBridge) source).bridge$getBukkitSender();
        RemoteServerCommandEvent event = new RemoteServerCommandEvent(sender, command);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return;
        }
        this.server.dispatchServerCommand(event.getSender(), new ConsoleInput(event.getCommand(), source));
    }

    @Inject(method = "onServerExit", at = @At("RETURN"))
    public void arclight$exitNow(CallbackInfo ci) {
        bridge$platform$exitNow();
        Thread exitThread = new Thread(this::arclight$exit, "Exit Thread");
        exitThread.setDaemon(true);
        exitThread.start();
    }

    private void arclight$exit() {
        try {
            Thread.sleep(5000L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        List<String> threads = new ArrayList<>();
        for (Thread thread : Thread.getAllStackTraces().keySet()) {
            if (!thread.isDaemon() && !thread.getName().equals("DestroyJavaVM")) {
                threads.add(thread.getName());
            }
        }
        if (!threads.isEmpty()) {
            ArclightServer.LOGGER.debug("Threads {} not shutting down", String.join(", ", threads));
            ArclightServer.LOGGER.info("{} threads not shutting down correctly, force exiting", threads.size());
        }
        System.exit(0);
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public String getPluginNames() {
        StringBuilder result = new StringBuilder();
        org.bukkit.plugin.Plugin[] plugins = server.getPluginManager().getPlugins();

        result.append(server.getName());
        result.append(" on Bukkit ");
        result.append(server.getBukkitVersion());

        if (plugins.length > 0 && server.getQueryPlugins()) {
            result.append(": ");

            for (int i = 0; i < plugins.length; i++) {
                if (i > 0) {
                    result.append("; ");
                }

                result.append(plugins[i].getDescription().getName());
                result.append(" ");
                result.append(plugins[i].getDescription().getVersion().replaceAll(";", ","));
            }
        }

        return result.toString();
    }

    @Override
    public WorldLoader.DataLoadContext arclight$dataLoadContext() {
        return this.worldLoader;
    }

    @Override
    public void arclight$forceUpgradeIfNeeded(LevelStorageSource.LevelStorageAccess worldSession, RegistryAccess.Frozen dimensions) {
        if (this.options.has("forceUpgrade")) {
            // TODO 1.21.11: Main.forceUpgrade is private; reintroduce force-upgrade support via invoker or copied upstream body.
        }
    }

    @Override
    public void arclight$prepareAndAddLevel(ServerLevel internal, PrimaryLevelData levelData) {
        this.initWorld(internal, levelData, levelData, levelData.worldGenOptions());
        internal.setSpawnSettings(true);
        this.addLevel(internal);
        this.prepareLevels(internal);
        internal.entityManager.tick();
    }
}
