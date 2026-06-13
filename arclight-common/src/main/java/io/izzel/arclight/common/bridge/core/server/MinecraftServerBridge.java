package io.izzel.arclight.common.bridge.core.server;

import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.TimeSource;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.RemoteConsoleCommandSender;
import org.bukkit.craftbukkit.CraftServer;

public interface MinecraftServerBridge {

    void bridge$setConsole(ConsoleCommandSender console);

    void bridge$setServer(CraftServer server);

    CraftServer bridge$getServer();

    RemoteConsoleCommandSender bridge$getRemoteConsole();

    void bridge$queuedProcess(Runnable runnable);

    void bridge$drainQueuedTasks();

    boolean bridge$hasStopped();

    Commands bridge$getVanillaCommands();

    void arclight$onServerLoad(ServerLevel level);

    void arclight$onServerUnload(ServerLevel level);

    default void bridge$forge$markLevelsDirty() {}

    default void bridge$forge$reinstatePersistentChunks(ServerLevel level, LongSet forcedChunks) {}

    default void bridge$forge$lockRegistries() {}

    default void bridge$forge$unlockRegistries() {}

    void arclight$extendNextTickTimeTo(TimeSource.NanoTimeSource timeSource);
}
