package io.izzel.arclight.common.bridge.core.server;

import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ForcedChunksSavedData;
import net.minecraft.world.level.Level;
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

    Commands bridge$getVanillaCommands();

    default void bridge$platform$loadLevel(Level level) {}

    default void bridge$platform$unloadLevel(Level level) {}

    default void bridge$forge$markLevelsDirty() {}

    default void bridge$platform$serverStarted() {}

    default void bridge$platform$serverStopping() {}

    default void bridge$forge$expectServerStopped() {}

    default void bridge$platform$serverStopped() {}

    default void bridge$forge$reinstatePersistentChunks(ServerLevel level, ForcedChunksSavedData savedData) {}

    default void bridge$forge$lockRegistries() {}

    default void bridge$forge$unlockRegistries() {}
}
