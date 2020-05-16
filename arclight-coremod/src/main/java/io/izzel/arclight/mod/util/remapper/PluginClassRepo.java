package io.izzel.arclight.mod.util.remapper;

import io.izzel.arclight.bridge.bukkit.JavaPluginLoaderBridge;
import net.md_5.specialsource.repo.ClassRepo;
import org.bukkit.plugin.java.JavaPluginLoader;
import org.objectweb.asm.tree.ClassNode;

import java.net.URLClassLoader;

public class PluginClassRepo implements ClassRepo {

    private final SharedClassRepo shared;
    private JavaPluginLoaderBridge loader;
    private URLClassLoader plugin;

    protected PluginClassRepo(SharedClassRepo shared, JavaPluginLoader loader, URLClassLoader plugin) {
        this.shared = shared;
        this.loader = (JavaPluginLoaderBridge) (Object) loader;
        this.plugin = plugin;
    }

    @Override
    public ClassNode findClass(String internalName) {
        if (plugin == null) return shared.findClass(internalName);
        if (loader.bridge$getLoaders().contains(plugin)) {
            plugin = null;
        }
        ClassNode node = shared.findClass(internalName);
        if (node == null && plugin != null) {
            return shared.findIn(plugin, internalName + ".class");
        }
        return node;
    }
}
