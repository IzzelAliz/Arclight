package io.izzel.arclight.common.mixin.bukkit;

import io.izzel.arclight.common.bridge.bukkit.CraftServerBridge;
import io.izzel.arclight.common.bridge.world.WorldBridge;
import io.izzel.arclight.common.bridge.world.server.ServerChunkProviderBridge;
import jline.console.ConsoleReader;
import net.minecraft.command.Commands;
import net.minecraft.server.dedicated.DedicatedPlayerList;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.management.PlayerList;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.craftbukkit.v.CraftServer;
import org.bukkit.craftbukkit.v.CraftWorld;
import org.bukkit.craftbukkit.v.command.BukkitCommandWrapper;
import org.bukkit.craftbukkit.v.command.CraftCommandMap;
import org.bukkit.craftbukkit.v.help.SimpleHelpMap;
import org.bukkit.craftbukkit.v.util.permissions.CraftDefaultPermissions;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginLoadOrder;
import org.bukkit.plugin.SimplePluginManager;
import org.bukkit.util.permissions.DefaultPermissions;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

@Mixin(value = CraftServer.class, remap = false)
public abstract class CraftServerMixin implements CraftServerBridge {

    // @formatter:off
    @Shadow(remap = false) @Final private CraftCommandMap commandMap;
    @Shadow(remap = false) @Final private SimplePluginManager pluginManager;
    @Shadow(remap = false) @Final private SimpleHelpMap helpMap;
    @Shadow(remap = false) protected abstract void enablePlugin(Plugin plugin);
    @Shadow(remap = false) protected abstract void loadCustomPermissions();
    @Shadow(remap = false) @Final protected DedicatedServer console;
    @Shadow(remap = false) @Final @Mutable private String serverName;
    @Accessor(value = "logger", remap = false) @Mutable public abstract void setLogger(Logger logger);
    @Shadow(remap = false) @Final @Mutable protected DedicatedPlayerList playerList;
    @Shadow public abstract World getWorld(UUID uid);
    @Shadow @Final private Map<String, World> worlds;
    @Shadow @Final private Logger logger;
    // @formatter:on

    @Inject(method = "<init>", at = @At("RETURN"))
    public void arclight$setBrand(DedicatedServer console, PlayerList playerList, CallbackInfo ci) {
        this.serverName = "Arclight";
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite(remap = false)
    public String getName() {
        return "Arclight";
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite(remap = false)
    public ConsoleReader getReader() {
        try {
            return new ConsoleReader();
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite(remap = false)
    public void enablePlugins(PluginLoadOrder type) {
        if (type == PluginLoadOrder.STARTUP) {
            helpMap.clear();
            helpMap.initializeGeneralTopics();
        }

        Plugin[] plugins = pluginManager.getPlugins();

        for (Plugin plugin : plugins) {
            if ((!plugin.isEnabled()) && (plugin.getDescription().getLoad() == type)) {
                enablePlugin(plugin);
            }
        }

        if (type == PluginLoadOrder.POSTWORLD) {
            this.commandMap.setFallbackCommands();
            this.commandMap.registerServerAliases();
            DefaultPermissions.registerCorePermissions();
            CraftDefaultPermissions.registerCorePermissions();
            this.loadCustomPermissions();
            this.helpMap.initializeCommands();
            this.syncCommands();
        }
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite(remap = false)
    private void syncCommands() {
        Commands dispatcher = this.console.getCommandManager();
        for (Map.Entry<String, Command> entry : this.commandMap.getKnownCommands().entrySet()) {
            String label = entry.getKey();
            Command command = entry.getValue();
            new BukkitCommandWrapper((CraftServer) (Object) this, command).register(dispatcher.getDispatcher(), label);
        }
    }

    @Override
    public void bridge$setPlayerList(PlayerList playerList) {
        this.playerList = (DedicatedPlayerList) playerList;
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public boolean unloadWorld(World world, boolean save) {
        if (world == null) {
            return false;
        }
        ServerWorld handle = ((CraftWorld) world).getHandle();
        if (!this.console.worlds.containsKey(handle.getDimension().getType())) {
            return false;
        } else if (handle.getDimension().getType() == DimensionType.OVERWORLD) {
            return false;
        } else if (handle.getPlayers().size() > 0) {
            return false;
        } else {
            WorldUnloadEvent e = new WorldUnloadEvent(((WorldBridge) handle).bridge$getWorld());
            this.pluginManager.callEvent(e);
            if (e.isCancelled()) {
                return false;
            } else {
                try {
                    if (save) {
                        handle.save(null, true, true);
                    }

                    MinecraftForge.EVENT_BUS.post(new WorldEvent.Unload(handle));
                    ((ServerChunkProviderBridge) handle.getChunkProvider()).bridge$close(save);
                } catch (Exception var6) {
                    this.logger.log(Level.SEVERE, null, var6);
                }

                this.worlds.remove(world.getName().toLowerCase(Locale.ENGLISH));
                this.console.worlds.remove(handle.getDimension().getType());
                return true;
            }
        }
    }

    @Override
    public void bridge$removeWorld(ServerWorld world) {
        if (world == null) {
            return;
        }
        this.worlds.remove(((WorldBridge) world).bridge$getWorld().getName().toLowerCase(Locale.ROOT));
    }
}
