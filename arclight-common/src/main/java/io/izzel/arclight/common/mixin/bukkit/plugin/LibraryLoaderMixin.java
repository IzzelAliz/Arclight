package io.izzel.arclight.common.mixin.bukkit.plugin;

import io.izzel.arclight.common.mod.util.remapper.generated.RemappingURLClassLoader;
import org.bukkit.plugin.PluginDescriptionFile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.List;

@Mixin(targets = "org.bukkit.plugin.java.LibraryLoader", remap = false)
public class LibraryLoaderMixin {

    // Replace only the default loader; leave Paper's custom factory branch in control.
    @Redirect(method = "createLoader(Lorg/bukkit/plugin/PluginDescriptionFile;Ljava/util/List;)Ljava/lang/ClassLoader;",
        at = @At(value = "NEW", target = "([Ljava/net/URL;Ljava/lang/ClassLoader;)Ljava/net/URLClassLoader;"))
    private URLClassLoader arclight$useRemapped(URL[] urls, ClassLoader loader, PluginDescriptionFile desc, List<Path> paperLibraryPaths) {
        return new RemappingURLClassLoader(String.format("%s/L", desc.getName()), urls, loader);
    }
}
