package io.izzel.arclight.common.mixin.bukkit.plugin;

import com.google.common.io.ByteStreams;
import io.izzel.arclight.common.bridge.bukkit.PluginClassLoaderBridge;
import io.izzel.arclight.common.mod.util.remapper.ArclightRemapConfig;
import io.izzel.arclight.common.mod.util.remapper.ArclightRemapper;
import io.izzel.arclight.common.mod.util.remapper.ClassLoaderRemapper;
import io.izzel.arclight.common.mod.util.remapper.RemappingClassLoader;
import io.izzel.arclight.i18n.ArclightConfig;
import io.izzel.tools.product.Product2;
import io.papermc.paper.plugin.entrypoint.classloader.group.LockingClassLoaderGroup;
import io.papermc.paper.plugin.entrypoint.classloader.group.SpigotPluginClassLoaderGroup;
import io.papermc.paper.plugin.provider.classloader.ConfiguredPluginClassLoader;
import io.papermc.paper.plugin.provider.classloader.PluginClassLoaderGroup;
import io.papermc.paper.plugin.provider.entrypoint.DependencyContext;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.SimplePluginManager;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.security.CodeSource;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.jar.Manifest;
import java.util.logging.Logger;

@Mixin(targets = "org.bukkit.plugin.java.PluginClassLoader", remap = false)
public abstract class PluginClassLoaderMixin extends URLClassLoader implements RemappingClassLoader, PluginClassLoaderBridge {

    // @formatter:off
    @Shadow @Final private Map<String, Class<?>> classes;
    @Shadow @Final private PluginDescriptionFile description;
    @Shadow @Final private Manifest manifest;
    @Shadow @Final private URL url;
    @Shadow @Final private ClassLoader libraryLoader;
    @Shadow private PluginClassLoaderGroup classLoaderGroup;
    @Shadow public DependencyContext dependencyContext;
    @Shadow abstract void setClass(@NotNull String name, @NotNull Class<?> clazz);
    // @formatter:on

    @Unique
    private ClassLoaderRemapper remapper;

    @Override
    public synchronized ClassLoaderRemapper getRemapper() {
        if (remapper == null) {
            remapper = ArclightRemapper.createClassLoaderRemapper(this);
        }
        return remapper;
    }

    @Override
    public ArclightRemapConfig getRemapConfig() {
        return ArclightRemapConfig.PLUGIN;
    }

    @Override
    public PluginDescriptionFile arclight$desc() {
        return description;
    }

    @Override
    public Class<?> arclight$loadFromExternal(String name, boolean initialize, boolean checkLibraries) throws ClassNotFoundException {
        return loadClass0(name, initialize, false, checkLibraries);
    }

    @Override
    public SimplePluginManager arclight$getPluginManager() {
        return (SimplePluginManager) Bukkit.getPluginManager();
    }

    @Override
    public Logger arclight$systemLogger() {
        return Bukkit.getLogger();
    }

    public PluginClassLoaderMixin(URL[] urls) {
        super(urls);
    }

    /**
     * @author InitAuther97
     * @reason Preserve Arclight isolation with Paper's dependency groups
     */
    @Overwrite
    Class<?> loadClass0(String name, boolean resolve, boolean checkGlobal, boolean checkLibraries) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            Class<?> c = findLoadedClass(name);
            if (c == null && getParent() != null && (!ArclightConfig.spec().getCompat().isAdventureIsolatedFromML() || !name.startsWith("net.kyori.adventure."))) {
                try {
                    c = getParent().loadClass(name);
                } catch (ClassNotFoundException ignored) {
                }
            }

            if (c == null) {
                try {
                    c = findClass(name);
                } catch (ClassNotFoundException ignored) {
                }
            }
            // Do not leak an already initiated dependency/library class into a
            // lookup whose group explicitly disabled that classpath (SPIGOT-6749).
            if (c != null && (checkGlobal || c.getClassLoader() == this)) {
                if (resolve) resolveClass(c);
                return c;
            }
        }

        if (checkLibraries && this.libraryLoader != null) {
            try {
                return this.libraryLoader.loadClass(name);
            } catch (ClassNotFoundException ignored) {
            }
        }

        if (checkGlobal) {
            Class<?> result = arclight$loadFromGroup(name, resolve);
            if (result != null) {
                return result;
            }
        }

        throw new ClassNotFoundException(String.format("Plugin %s cannot load class %s", description.getName(), name));
    }

    @Unique
    private Class<?> arclight$loadFromGroup(String name, boolean resolve) {
        ConfiguredPluginClassLoader requester = (ConfiguredPluginClassLoader) (Object) this;
        if (!ArclightConfig.spec().getCompat().isIsolatedPluginClassLoaders(name)) {
            return this.classLoaderGroup.getClassByName(name, resolve, requester);
        }

        // Filter before loading: rejecting the first global result afterwards
        // would hide a later dependency providing the same class.
        // Follow only this owner's live Spigot view, never an independent global
        // cache that could remain accessible after storage unregisters the loader.
        if (this.classLoaderGroup instanceof LockingClassLoaderGroup lockingGroup
            && lockingGroup.getParent() instanceof SpigotPluginClassLoaderGroup spigotGroup) {
            for (ConfiguredPluginClassLoader candidate : spigotGroup.getClassLoaders()) {
                if (candidate == requester || this.dependencyContext.isTransitiveDependency(this.description, candidate.getConfiguration())) {
                    try {
                        return candidate.loadClass(name, resolve, false, true);
                    } catch (ClassNotFoundException ignored) {
                    }
                }
            }
        }
        return null;
    }

    /**
     * @author IzzelAliz
     * @reason Keep resources local to the plugin and its own libraries
     */
    @Overwrite
    @Override
    public URL getResource(String name) {
        Objects.requireNonNull(name);
        URL resource = findResource(name);
        if (resource == null && this.libraryLoader != null) {
            resource = this.libraryLoader.getResource(name);
        }
        return resource;
    }

    /**
     * @author IzzelAliz
     * @reason Keep resource enumeration local to the plugin and its own libraries
     */
    @Overwrite
    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        Objects.requireNonNull(name);
        List<URL> resources = Collections.list(findResources(name));
        if (this.libraryLoader != null) {
            resources.addAll(Collections.list(this.libraryLoader.getResources(name)));
        }
        return Collections.enumeration(resources);
    }

    /**
     * @author IzzelAliz
     * @reason Transform plugin bytecode before definition and retain Paper serialization registration
     */
    @Overwrite
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        if (name.startsWith("org.bukkit.") || name.startsWith("net.minecraft.")) {
            throw new ClassNotFoundException(name);
        }
        Class<?> result = classes.get(name);

        if (result == null) {
            String path = name.replace('.', '/').concat(".class");
            URL url = this.findResource(path);

            if (url != null) {
                URLConnection connection;
                Callable<byte[]> byteSource;
                try {
                    connection = url.openConnection();
                    connection.connect();
                    byteSource = () -> {
                        try (InputStream is = connection.getInputStream()) {
                            byte[] classBytes = ByteStreams.toByteArray(is);
                            classBytes = ArclightRemapper.SWITCH_TABLE_FIXER.apply(classBytes);
                            classBytes = Bukkit.getUnsafe().processClass(description, path, classBytes);
                            return classBytes;
                        }
                    };
                } catch (IOException e) {
                    throw new ClassNotFoundException(name, e);
                }

                Product2<byte[], CodeSource> classBytes = this.getRemapper().remapClass(name, byteSource, connection, ArclightRemapConfig.PLUGIN);

                int dot = name.lastIndexOf('.');
                if (dot != -1) {
                    String pkgName = name.substring(0, dot);
                    if (getPackage(pkgName) == null) {
                        try {
                            if (manifest != null) {
                                definePackage(pkgName, manifest, this.url);
                            } else {
                                definePackage(pkgName, null, null, null, null, null, null, null);
                            }
                        } catch (IllegalArgumentException ex) {
                            if (getPackage(pkgName) == null) {
                                throw new IllegalStateException("Cannot find package " + pkgName);
                            }
                        }
                    }
                }

                result = defineClass(name, classBytes._1, 0, classBytes._1.length, classBytes._2);
            }

            if (result == null) {
                result = super.findClass(name);
            }

            classes.put(name, result);
            this.setClass(name, result);
        }

        return result;
    }
}
