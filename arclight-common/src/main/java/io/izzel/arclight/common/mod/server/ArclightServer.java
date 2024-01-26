package io.izzel.arclight.common.mod.server;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.izzel.arclight.common.bridge.bukkit.CraftServerBridge;
import io.izzel.arclight.common.bridge.core.server.MinecraftServerBridge;
import io.izzel.arclight.common.mod.util.VelocitySupport;
import io.izzel.arclight.common.mod.util.log.ArclightI18nLogger;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.level.dimension.LevelStem;
import org.apache.logging.log4j.Logger;
import org.bukkit.World;
import org.bukkit.craftbukkit.v.CraftServer;
import org.bukkit.craftbukkit.v.command.ColouredConsoleSender;
import org.jetbrains.annotations.NotNull;
import org.spigotmc.SpigotConfig;

import java.io.File;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Supplier;

public class ArclightServer {

    public static final Logger LOGGER = ArclightI18nLogger.getLogger("Arclight");

    private interface ExecutorWithThread extends Executor, Supplier<Thread> {
    }

    private static final ExecutorWithThread mainThreadExecutor = new ExecutorWithThread() {
        @Override
        public void execute(@NotNull Runnable command) {
            executeOnMainThread(command);
        }

        @Override
        public Thread get() {
            return getMinecraftServer().getRunningThread();
        }
    };
    private static final ExecutorService chatExecutor = Executors.newCachedThreadPool(
        new ThreadFactoryBuilder().setDaemon(true).setNameFormat("Async Chat Thread - #%d")
            .setThreadFactory(chatFactory()).build());

    private static ThreadFactory chatFactory() {
        var group = Thread.currentThread().getThreadGroup();
        var classLoader = Thread.currentThread().getContextClassLoader();
        return r -> {
            var thread = new Thread(group, r);
            thread.setContextClassLoader(classLoader);
            return thread;
        };
    }

    private static CraftServer server;

    @SuppressWarnings("ConstantConditions")
    public static CraftServer createOrLoad(DedicatedServer console, PlayerList playerList) {
        if (server == null) {
            try {
                server = new CraftServer(console, playerList);
                ((MinecraftServerBridge) console).bridge$setServer(server);
                ((MinecraftServerBridge) console).bridge$setConsole(ColouredConsoleSender.getInstance());

                Class.forName("org.sqlite.JDBC");
                Class.forName("com.mysql.cj.jdbc.Driver");
            } catch (Throwable t) {
                throw new RuntimeException("Error initializing Arclight", t);
            }
            try {
                LOGGER.info("registry.begin");
                BukkitRegistry.registerAll(console);
                org.spigotmc.SpigotConfig.init(new File("./spigot.yml"));
                org.spigotmc.SpigotConfig.registerCommands();
                if (VelocitySupport.isEnabled()) {
                    SpigotConfig.bungee = true;
                }
            } catch (Throwable t) {
                LOGGER.error("registry.error", t);
                throw t;
            }
        } else {
            ((CraftServerBridge) (Object) server).bridge$setPlayerList(playerList);
        }
        return server;
    }

    public static CraftServer get() {
        return Objects.requireNonNull(server);
    }

    public static boolean isPrimaryThread() {
        if (server == null) {
            return Thread.currentThread().equals(getMinecraftServer().getRunningThread());
        } else {
            return server.isPrimaryThread();
        }
    }

    private static MinecraftServer vanillaServer;

    public static void setMinecraftServer(MinecraftServer server) {
        vanillaServer = server;
    }

    public static MinecraftServer getMinecraftServer() {
        return Objects.requireNonNull(vanillaServer, "vanillaServer");
    }

    public static void executeOnMainThread(Runnable runnable) {
        ((MinecraftServerBridge) getMinecraftServer()).bridge$queuedProcess(runnable);
        if (LockSupport.getBlocker(getMinecraftServer().getRunningThread()) == "waiting for tasks") {
            LockSupport.unpark(getMinecraftServer().getRunningThread());
        }
    }

    public static Executor getMainThreadExecutor() {
        return mainThreadExecutor;
    }

    public static ExecutorService getChatExecutor() {
        return chatExecutor;
    }

    public static World.Environment getEnvironment(ResourceKey<LevelStem> key) {
        return BukkitRegistry.DIM_MAP.getOrDefault(key, World.Environment.CUSTOM);
    }
}
