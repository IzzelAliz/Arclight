package io.izzel.arclight.common.mixin.core.server.dedicated;

import io.izzel.arclight.common.bridge.core.server.dedicated.DedicatedServerBridge;
import io.izzel.arclight.common.mixin.core.server.MinecraftServerMixin;
import io.izzel.arclight.common.mod.server.ArclightServer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.ConsoleInput;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.rcon.RconConsoleSource;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.CraftServer;
import org.bukkit.craftbukkit.v.command.CraftRemoteConsoleCommandSender;
import org.bukkit.event.server.RemoteServerCommandEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.plugin.PluginLoadOrder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(DedicatedServer.class)
public abstract class DedicatedServerMixin extends MinecraftServerMixin implements DedicatedServerBridge {

    // @formatter:off
    @Shadow @Final public RconConsoleSource rconConsoleSource;
    // @formatter:on

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

    @Inject(method = "initServer", at = @At(value = "FIELD", target = "Lnet/minecraft/server/dedicated/DedicatedServerProperties;enableRcon:Z"))
    public void arclight$setRcon(CallbackInfoReturnable<Boolean> cir) {
        this.remoteConsole = new CraftRemoteConsoleCommandSender(this.rconConsoleSource);
    }

    @Redirect(method = "handleConsoleInputs", at = @At(value = "INVOKE", target = "Lnet/minecraft/commands/Commands;performPrefixedCommand(Lnet/minecraft/commands/CommandSourceStack;Ljava/lang/String;)V"))
    private void arclight$serverCommandEvent(Commands commands, CommandSourceStack source, String command) {
        if (command.isEmpty()) {
            return;
        }
        ServerCommandEvent event = new ServerCommandEvent(console, command);
        Bukkit.getPluginManager().callEvent(event);
        if (!event.isCancelled()) {
            server.dispatchServerCommand(console, new ConsoleInput(event.getCommand(), source));
        }
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public String runCommand(String command) {
        this.rconConsoleSource.prepareForCommand();
        this.executeBlocking(() -> {
            RemoteServerCommandEvent event = new RemoteServerCommandEvent(remoteConsole, command);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                return;
            }
            this.server.dispatchServerCommand(remoteConsole, new ConsoleInput(event.getCommand(), this.rconConsoleSource.createCommandSourceStack()));
        });
        return this.rconConsoleSource.getCommandResponse();
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
}
