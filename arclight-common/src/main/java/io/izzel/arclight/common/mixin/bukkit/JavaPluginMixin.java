package io.izzel.arclight.common.mixin.bukkit;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginLogger;
import org.bukkit.plugin.java.JavaPlugin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import io.izzel.arclight.common.mod.util.ArclightPluginLogger;

@Mixin(JavaPlugin.class)
public class JavaPluginMixin {

    @Redirect(method = "init", remap = false, at = @At(value = "NEW", target = "org/bukkit/plugin/PluginLogger"))
    private PluginLogger arclight$createLogger(Plugin plugin) {
        return new ArclightPluginLogger(plugin);
    }

}
