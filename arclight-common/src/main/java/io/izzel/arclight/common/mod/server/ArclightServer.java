package io.izzel.arclight.common.mod.server;

import io.izzel.arclight.api.Arclight;
import io.izzel.arclight.common.bridge.bukkit.CraftServerBridge;
import io.izzel.arclight.common.bridge.core.server.MinecraftServerBridge;
import io.izzel.arclight.common.mod.ArclightMod;
import io.izzel.arclight.common.mod.server.api.DefaultArclightServer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.bukkit.World;
import org.bukkit.craftbukkit.v.CraftServer;
import org.bukkit.craftbukkit.v.command.ColouredConsoleSender;

import java.io.File;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.LockSupport;

public class ArclightServer {

    private static final Executor mainThreadExecutor = ArclightServer::executeOnMainThread;
    private static CraftServer server;

    @SuppressWarnings("ConstantConditions")
    public static CraftServer createOrLoad(DedicatedServer console, PlayerList playerList) {
        if (server == null) {
            Arclight.setServer(new DefaultArclightServer());
            try {
                server = new CraftServer(console, playerList);
                ((MinecraftServerBridge) console).bridge$setServer(server);
                ((MinecraftServerBridge) console).bridge$setConsole(ColouredConsoleSender.getInstance());
                ArclightPermissionHandler.INSTANCE.initialize();
            } catch (Throwable t) {
                t.printStackTrace();
            }
            try {
                ArclightMod.LOGGER.info("registry.begin");
                BukkitRegistry.registerAll();
                org.spigotmc.SpigotConfig.init(new File("./spigot.yml"));
                org.spigotmc.SpigotConfig.registerCommands();
            } catch (Throwable t) {
                ArclightMod.LOGGER.error("registry.error", t);
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

    public static MinecraftServer getMinecraftServer() {
        return ServerLifecycleHooks.getCurrentServer();
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

    public static World.Environment getEnvironment(ResourceKey<LevelStem> key) {
        return BukkitRegistry.DIM_MAP.getOrDefault(key, World.Environment.CUSTOM);
    }
}
