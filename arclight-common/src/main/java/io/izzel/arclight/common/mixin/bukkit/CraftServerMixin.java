package io.izzel.arclight.common.mixin.bukkit;

import jline.console.ConsoleReader;
import net.minecraft.command.Commands;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.management.PlayerList;
import org.bukkit.command.Command;
import org.bukkit.craftbukkit.v.CraftServer;
import org.bukkit.craftbukkit.v.command.BukkitCommandWrapper;
import org.bukkit.craftbukkit.v.command.CraftCommandMap;
import org.bukkit.craftbukkit.v.help.SimpleHelpMap;
import org.bukkit.craftbukkit.v.util.permissions.CraftDefaultPermissions;
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
import java.util.Map;
import java.util.logging.Logger;

@Mixin(CraftServer.class)
public abstract class CraftServerMixin {

    // @formatter:off
    @Shadow(remap = false) @Final private CraftCommandMap commandMap;
    @Shadow(remap = false) @Final private SimplePluginManager pluginManager;
    @Shadow(remap = false) @Final private SimpleHelpMap helpMap;
    @Shadow(remap = false) protected abstract void enablePlugin(Plugin plugin);
    @Shadow(remap = false) protected abstract void loadCustomPermissions();
    @Shadow(remap = false) @Final protected DedicatedServer console;
    @Shadow(remap = false) @Final @Mutable private String serverName;
    @Accessor(value = "logger", remap = false) @Mutable public abstract void setLogger(Logger logger);
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

}
