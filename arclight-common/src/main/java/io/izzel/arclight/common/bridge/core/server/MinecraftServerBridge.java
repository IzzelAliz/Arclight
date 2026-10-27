package io.izzel.arclight.common.bridge.core.server;

import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.TimeSource;
import net.minecraft.world.level.TicketStorage;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.RemoteConsoleCommandSender;
import org.bukkit.craftbukkit.CraftServer;

public interface MinecraftServerBridge {

    void bridge$setDifficulty(ServerLevel level, net.minecraft.world.Difficulty difficulty,
                              net.minecraft.commands.CommandSourceStack source, boolean ignoreLock);
    joptsimple.OptionSet bridge$getOptions();
    void bridge$setAutosavePeriod(int period);

    void bridge$setConsole(ConsoleCommandSender console);

    ConsoleCommandSender bridge$getConsole();

    void bridge$setServer(CraftServer server);

    CraftServer bridge$getServer();

    RemoteConsoleCommandSender bridge$getRemoteConsole();

    void bridge$queuedProcess(Runnable runnable);

    void bridge$drainQueuedTasks();

    boolean bridge$hasStopped();

    void bridge$safeShutdown(boolean waitForServer, boolean restarting);

    boolean bridge$isRestarting();

    int bridge$getCurrentTick();

    double[] bridge$getRecentTps();

    net.kyori.adventure.text.Component bridge$getMotd();

    void bridge$setMotd(net.kyori.adventure.text.Component motd);

    boolean bridge$isTickPaused();

    Commands bridge$getVanillaCommands();

    void arclight$onServerLoad(ServerLevel level);

    void arclight$onServerUnload(ServerLevel level);

    void bridge$removeLevel(ServerLevel level);

    /** Compensate a failed createWorld only while its exact level is still published. */
    void arclight$rollbackPublishedLevel(ServerLevel level);

    default void bridge$forge$markLevelsDirty() {}

    default void bridge$forge$reinstatePersistentChunks(ServerLevel level, TicketStorage savedData) {}

    default void bridge$forge$lockRegistries() {}

    default void bridge$forge$unlockRegistries() {}

    void arclight$extendNextTickTimeTo(TimeSource.NanoTimeSource timeSource);

    default boolean hasStopped() {
        return this.bridge$hasStopped();
    }

    default double[] getTPS() {
        return this.bridge$getRecentTps();
    }

    default net.kyori.adventure.text.Component motd() {
        return this.bridge$getMotd();
    }

    default void motd(net.kyori.adventure.text.Component motd) {
        this.bridge$setMotd(motd);
    }

    default boolean isTickPaused() {
        return this.bridge$isTickPaused();
    }

    default void removeLevel(net.minecraft.server.level.ServerLevel level) {
        this.bridge$removeLevel(level);
    }

    default void safeShutdown(boolean waitForServer, boolean restarting) {
        this.bridge$safeShutdown(waitForServer, restarting);
    }

}
