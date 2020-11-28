package io.izzel.arclight.common.bridge.server;

import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.RemoteConsoleCommandSender;
import org.bukkit.craftbukkit.v.CraftServer;

public interface MinecraftServerBridge {

    void bridge$setAutosavePeriod(int autosavePeriod);

    void bridge$setConsole(ConsoleCommandSender console);

    void bridge$setServer(CraftServer server);

    RemoteConsoleCommandSender bridge$getRemoteConsole();

    void bridge$setRemoteConsole(RemoteConsoleCommandSender sender);

    void bridge$queuedProcess(Runnable runnable);

    void bridge$drainQueuedTasks();

    boolean bridge$hasStopped();
}
