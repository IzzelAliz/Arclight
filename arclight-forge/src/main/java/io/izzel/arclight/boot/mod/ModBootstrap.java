package io.izzel.arclight.boot.mod;

import cpw.mods.cl.JarModuleFinder;
import cpw.mods.cl.ModuleClassLoader;
import cpw.mods.jarhandling.SecureJar;
import cpw.mods.jarhandling.impl.Jar;
import cpw.mods.modlauncher.LaunchPluginHandler;
import cpw.mods.modlauncher.Launcher;
import cpw.mods.modlauncher.serviceapi.ILaunchPluginService;
import io.izzel.arclight.api.Unsafe;
import io.izzel.arclight.boot.AbstractBootstrap;
import io.izzel.arclight.boot.asm.ArclightImplementer;
import io.izzel.arclight.forgeinstaller.ForgeInstaller;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.MarkerManager;

import java.io.File;
import java.lang.invoke.MethodType;
import java.lang.module.Configuration;
import java.lang.module.ModuleFinder;
import java.lang.module.ResolvedModule;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ModBootstrap extends AbstractBootstrap {

    public static record ModBoot(Configuration configuration, ClassLoader parent) {}

    private static ModBoot modBoot;

    static void run() {
        var plugin = Launcher.INSTANCE.environment().findLaunchPlugin("arclight_implementer");
        if (plugin.isPresent()) return;
        var logger = LogManager.getLogger("Arclight");
        var marker = MarkerManager.getMarker("INSTALL");
        try {
            var paths = ForgeInstaller.modInstall(s -> logger.info(marker, s));
            load(paths.toArray(new Path[0]));
            new ModBootstrap().inject();
        } catch (Throwable e) {
            logger.error("Error bootstrap Arclight", e);
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public static void postRun() {
        if (modBoot == null) return;
        try {
            var conf = modBoot.configuration();
            var parent = modBoot.parent();
            var classLoader = (ModuleClassLoader) Thread.currentThread().getContextClassLoader();
            var parentField = ModuleClassLoader.class.getDeclaredField("parentLoaders");
            var parentLoaders = (Map<String, ClassLoader>) Unsafe.getObject(classLoader, Unsafe.objectFieldOffset(parentField));
            for (var mod : conf.modules()) {
                for (var pk : mod.reference().descriptor().packages()) {
                    parentLoaders.put(pk, parent);
                }
            }
            modBoot = null;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    private void inject() throws Throwable {
        dirtyHacks();
        setupMod();
        injectClassPath();
        injectLaunchPlugin();
    }

    private void injectClassPath() throws Throwable {
        var platform = ClassLoader.getPlatformClassLoader();
        var ucpField = platform.getClass().getSuperclass().getDeclaredField("ucp");
        var ucp = Unsafe.lookup().unreflectGetter(ucpField).invoke(platform);
        if (ucp == null) {
            for (var module : ModuleLayer.boot().configuration().modules()) {
                var optional = module.reference().location();
                if (optional.isPresent()) {
                    var uri = optional.get();
                    if (uri.getScheme().equals("file")) {
                        ForgeInstaller.addToPath(new File(uri).toPath());
                    }
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void injectLaunchPlugin() throws Exception {
        var instance = Launcher.INSTANCE;
        var launchPlugins = Launcher.class.getDeclaredField("launchPlugins");
        launchPlugins.setAccessible(true);
        var handler = (LaunchPluginHandler) launchPlugins.get(instance);
        var plugins = LaunchPluginHandler.class.getDeclaredField("plugins");
        plugins.setAccessible(true);
        var map = (Map<String, ILaunchPluginService>) plugins.get(handler);
        var transformLogger = !(java.util.logging.LogManager.getLogManager() instanceof org.apache.logging.log4j.jul.LogManager);
        if (transformLogger && !System.getProperties().contains("log4j.jul.LoggerAdapter")) {
            System.setProperty("log4j.jul.LoggerAdapter", "io.izzel.arclight.boot.log.ArclightLoggerAdapter");
        }
        var plugin = new ArclightImplementer(transformLogger);
        map.put(plugin.name(), plugin);
    }

    private static final Set<String> EXCLUDES = Set.of("org/apache/maven/artifact/repository/metadata");

    @SuppressWarnings("unchecked")
    private static void load(Path[] file) throws Throwable {
        var classLoader = (ModuleClassLoader) ModBootstrap.class.getClassLoader();
        var secureJar = SecureJar.from((path, base) -> EXCLUDES.stream().noneMatch(path::startsWith), file);
        var configurationField = ModuleClassLoader.class.getDeclaredField("configuration");
        var confOffset = Unsafe.objectFieldOffset(configurationField);
        var oldConf = (Configuration) Unsafe.getObject(classLoader, confOffset);
        var conf = oldConf.resolveAndBind(JarModuleFinder.of(secureJar), ModuleFinder.of(), List.of(secureJar.name()));
        modBoot = new ModBoot(conf, classLoader);
        Unsafe.putObjectVolatile(classLoader, confOffset, conf);
        var pkgField = ModuleClassLoader.class.getDeclaredField("packageLookup");
        var packageLookup = (Map<String, ResolvedModule>) Unsafe.getObject(classLoader, Unsafe.objectFieldOffset(pkgField));
        var rootField = ModuleClassLoader.class.getDeclaredField("resolvedRoots");
        var resolvedRoots = (Map<String, Object>) Unsafe.getObject(classLoader, Unsafe.objectFieldOffset(rootField));
        var moduleRefCtor = Unsafe.lookup().findConstructor(Class.forName("cpw.mods.cl.JarModuleFinder$JarModuleReference"),
            MethodType.methodType(void.class, Jar.class));
        for (var mod : conf.modules()) {
            for (var pk : mod.reference().descriptor().packages()) {
                packageLookup.put(pk, mod);
            }
            resolvedRoots.put(mod.name(), moduleRefCtor.invokeWithArguments(secureJar));
        }
    }
}
