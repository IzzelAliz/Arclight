package io.izzel.arclight.common.mixin.bukkit.plugin;

import io.izzel.arclight.common.mod.util.log.ArclightPluginLogger;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.logging.Logger;

@Mixin(value = JavaPlugin.class, remap = false)
public class JavaPluginMixin {

    @Shadow private Logger logger;

    @Inject(method = "init(Lorg/bukkit/Server;Lorg/bukkit/plugin/PluginDescriptionFile;Ljava/io/File;Ljava/io/File;Ljava/lang/ClassLoader;Lio/papermc/paper/plugin/configuration/PluginMeta;Ljava/util/logging/Logger;)V", remap = false, at = @At("RETURN"))
    private void arclight$createLogger(CallbackInfo ci) {
        // Both provider-created plugins and the legacy init overload arrive
        // here, after the description/server needed by PluginLogger are set.
        this.logger = new ArclightPluginLogger((Plugin) (Object) this);
    }

}
