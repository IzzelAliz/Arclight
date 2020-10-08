package io.izzel.arclight.common.mod.server;

import io.izzel.arclight.common.bridge.bukkit.CraftServerBridge;
import io.izzel.arclight.common.bridge.server.MinecraftServerBridge;
import io.izzel.arclight.common.mod.ArclightMod;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.management.PlayerList;
import net.minecraft.util.RegistryKey;
import net.minecraft.world.DimensionType;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import org.bukkit.World;
import org.bukkit.craftbukkit.v.CraftServer;
import org.bukkit.craftbukkit.v.command.ColouredConsoleSender;

import java.io.File;
import java.util.Objects;
import java.util.concurrent.Executor;

public class ArclightServer {

    private static final Executor mainThreadExecutor = ArclightServer::executeOnMainThread;
    private static CraftServer server;

    @SuppressWarnings("ConstantConditions")
    public static CraftServer createOrLoad(DedicatedServer console, PlayerList playerList) {
        if (server == null) {
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
            return Thread.currentThread().equals(getMinecraftServer().getExecutionThread());
        } else {
            return server.isPrimaryThread();
        }
    }

    public static MinecraftServer getMinecraftServer() {
        return ServerLifecycleHooks.getCurrentServer();
    }

    public static void executeOnMainThread(Runnable runnable) {
        ((MinecraftServerBridge) getMinecraftServer()).bridge$queuedProcess(runnable);
    }

    public static Executor getMainThreadExecutor() {
        return mainThreadExecutor;
    }

    public static World.Environment getEnvironment(RegistryKey<DimensionType> key) {
        return BukkitRegistry.DIM_MAP.get(key);
    }

    public static RegistryKey<DimensionType> getDimensionType(World.Environment environment) {
        return BukkitRegistry.DIM_MAP.inverse().get(environment);
    }
}
