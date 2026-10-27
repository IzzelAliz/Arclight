package io.izzel.arclight.common.mixin.core.server.dedicated;

import io.izzel.arclight.common.bridge.core.command.CommandSourceBridge;
import io.izzel.arclight.common.bridge.core.server.dedicated.DedicatedServerBridge;
import io.izzel.arclight.common.mixin.core.server.MinecraftServerMixin;
import io.izzel.arclight.common.mod.server.ArclightServer;
import io.izzel.arclight.common.mod.server.world.WorldCreationContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.ConsoleInput;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.WorldLoader;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.rcon.RconConsoleSource;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.PrimaryLevelData;
import net.minecraft.world.level.storage.WorldData;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.conversations.Conversable;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.event.server.RemoteServerCommandEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.plugin.PluginLoadOrder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

@Mixin(DedicatedServer.class)
public abstract class DedicatedServerMixin extends MinecraftServerMixin implements DedicatedServerBridge {

    public DedicatedServerMixin(String name, boolean propagatesCrashes) {
        super(name, propagatesCrashes);
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
     * @reason Preserve the vanilla RCON entry point with Bukkit events and isolated responses.
     */
    @Overwrite
    public String runCommand(String command) {
        RconConsoleSource source = new RconConsoleSource((MinecraftServer) (Object) this);
        ((io.izzel.arclight.common.bridge.core.server.rcon.RconConsoleSourceBridge) source).bridge$setSocketAddress(null);
        return this.bridge$runRconCommand(source, command);
    }

    @Override
    public String bridge$runRconCommand(RconConsoleSource source, String command) {
        source.prepareForCommand();
        this.executeBlocking(() -> {
            CommandSourceStack wrapper = source.createCommandSourceStack();
            RemoteServerCommandEvent event = new RemoteServerCommandEvent(((CommandSourceBridge) source).bridge$getBukkitSender(wrapper), command);
            this.server.getPluginManager().callEvent(event);
            if (!event.isCancelled()) {
                this.arclight$dispatchServerCommand(event.getSender(), event.getCommand());
            }
        });
        return source.getCommandResponse();
    }

    @Shadow @Final private List<ConsoleInput> consoleInput;
    @Shadow @Final @org.spongepowered.asm.mixin.Mutable private net.minecraft.server.dedicated.DedicatedServerSettings settings;

    @Override
    public net.minecraft.server.dedicated.DedicatedServerProperties bridge$reloadProperties() {
        this.settings = new net.minecraft.server.dedicated.DedicatedServerSettings(
            ((io.izzel.arclight.common.bridge.core.server.dedicated.DedicatedServerSettingsBridge) this.settings).bridge$getSource());
        return this.settings.getProperties();
    }


    /**
     * @author IzzelAliz
     * @reason Process queued console commands through Bukkit on the server thread.
     */
    @Overwrite
    public void handleConsoleInputs() {
        while (!this.consoleInput.isEmpty()) {
            ConsoleInput input = this.consoleInput.remove(0);
            ServerCommandEvent event = new ServerCommandEvent(this.console, input.msg);
            this.server.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                continue;
            }
            this.arclight$dispatchServerCommand(this.console, event.getCommand());
        }
    }

    @Unique
    private boolean arclight$dispatchServerCommand(CommandSender sender, String command) {
        if (sender instanceof Conversable conversable && conversable.isConversing()) {
            conversable.acceptConversationInput(command);
            return true;
        }
        try {
            return this.server.dispatchCommand(sender, command);
        } catch (Exception ex) {
            this.server.getLogger().log(Level.WARNING, "Unexpected exception while parsing console command \"" + command + '"', ex);
            return false;
        }
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
    public void arclight$forceUpgradeIfNeeded(LevelStorageSource.LevelStorageAccess worldSession, WorldData worldData, RegistryAccess.Frozen dimensions) {
        if (this.options.has("forceUpgrade") || this.options.has("recreateRegionFiles")) {
            io.izzel.arclight.common.mod.server.WorldUpgradeHelper.forceUpgrade(
                worldSession, DataFixers.getDataFixer(), this.options.has("eraseCache"),
                () -> true, dimensions, this.options.has("recreateRegionFiles"));
        }
    }

    @Override
    public void arclight$prepareAndAddLevel(ServerLevel internal, PrimaryLevelData levelData) {
        this.initWorld(internal, levelData, levelData, WorldCreationContext.require(internal).options());
        internal.setSpawnSettings(true);
        this.addLevel(internal);
        this.prepareLevels(internal);
        internal.entityManager.tick();
    }

    @Override
    public void arclight$rollbackPublishedLevel(ServerLevel level) {
        // addLevel puts into levels before notifying platform listeners, which may throw
        // or create a replacement level at the same key. Never remove that replacement.
        if (((MinecraftServer) (Object) this).getLevel(level.dimension()) == level) {
            this.removeLevel(level);
        }
    }
}
