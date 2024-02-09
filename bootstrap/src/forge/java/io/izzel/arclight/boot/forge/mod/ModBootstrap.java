package io.izzel.arclight.boot.forge.mod;

import cpw.mods.jarhandling.SecureJar;
import cpw.mods.jarhandling.impl.Jar;
import cpw.mods.modlauncher.LaunchPluginHandler;
import cpw.mods.modlauncher.Launcher;
import cpw.mods.modlauncher.serviceapi.ILaunchPluginService;
import cpw.mods.util.LambdaExceptionUtils;
import io.izzel.arclight.api.ArclightPlatform;
import io.izzel.arclight.api.Unsafe;
import io.izzel.arclight.boot.AbstractBootstrap;
import io.izzel.arclight.installer.ForgeInstaller;
import io.izzel.arclight.installer.MinecraftProvider;
import net.minecraftforge.securemodules.SecureModuleClassLoader;
import net.minecraftforge.securemodules.SecureModuleFinder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.MarkerManager;

import java.io.File;
import java.io.InputStream;
import java.lang.invoke.MethodType;
import java.lang.module.Configuration;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ResolvedModule;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSigner;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.jar.Manifest;

public class ModBootstrap implements AbstractBootstrap {

    public record ModBoot(Configuration configuration, ClassLoader parent) {}

    private static ModBoot modBoot;

    static void run() {
        var plugin = Launcher.INSTANCE.environment().findLaunchPlugin("arclight_implementer");
        if (plugin.isPresent()) return;
        var logger = LogManager.getLogger("Arclight");
        var marker = MarkerManager.getMarker("INSTALL");
        try {
            var paths = MinecraftProvider.modInstall(s -> logger.info(marker, s));
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
            var classLoader = (SecureModuleClassLoader) Thread.currentThread().getContextClassLoader();
            var parentField = SecureModuleClassLoader.class.getDeclaredField("packageToParentLoader");
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
        setupMod(ArclightPlatform.FORGE);
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
        var plugin = new ArclightImplementer();
        map.put(plugin.name(), plugin);
    }

    private static final Set<String> EXCLUDES = Set.of("org/apache/maven/artifact/repository/metadata");

    @SuppressWarnings("unchecked")
    private static void load(Path[] file) throws Throwable {
        var classLoader = (SecureModuleClassLoader) ModBootstrap.class.getClassLoader();
        var secureJar = SecureJar.from((path, base) -> EXCLUDES.stream().noneMatch(path::startsWith), file);
        var configurationField = SecureModuleClassLoader.class.getDeclaredField("configuration");
        var confOffset = Unsafe.objectFieldOffset(configurationField);
        var oldConf = (Configuration) Unsafe.getObject(classLoader, confOffset);
        var conf = oldConf.resolveAndBind(SecureModuleFinder.of(secureJar), ModuleFinder.of(), List.of(secureJar.name()));
        modBoot = new ModBoot(conf, classLoader);
        Unsafe.putObjectVolatile(classLoader, confOffset, conf);
        var pkgField = SecureModuleClassLoader.class.getDeclaredField("packageToOurModules");
        var packageLookup = (Map<String, ResolvedModule>) Unsafe.getObject(classLoader, Unsafe.objectFieldOffset(pkgField));
        var rootField = SecureModuleClassLoader.class.getDeclaredField("ourModulesSecure");
        var resolvedRoots = (Map<String, Object>) Unsafe.getObject(classLoader, Unsafe.objectFieldOffset(rootField));
        var moduleRefCtor = Unsafe.lookup().findConstructor(Class.forName("net.minecraftforge.securemodules.SecureModuleFinder$Reference"),
            MethodType.methodType(void.class, SecureJar.ModuleDataProvider.class));
        for (var mod : conf.modules()) {
            for (var pk : mod.reference().descriptor().packages()) {
                packageLookup.put(pk, mod);
            }
            resolvedRoots.put(mod.name(), moduleRefCtor.invokeWithArguments(new JarModuleDataProvider((Jar) secureJar)));
        }
    }

    private record JarModuleDataProvider(Jar jar) implements SecureJar.ModuleDataProvider {

        @Override
        public String name() {
            return jar.name();
        }

        @Override
        public ModuleDescriptor descriptor() {
            return jar.computeDescriptor();
        }

        @Override
        public URI uri() {
            return jar.getURI();
        }

        @Override
        public Optional<URI> findFile(final String name) {
            return jar.findFile(name);
        }

        @Override
        public Optional<InputStream> open(final String name) {
            return jar.findFile(name).map(Paths::get).map(LambdaExceptionUtils.rethrowFunction(Files::newInputStream));
        }

        @Override
        public Manifest getManifest() {
            return jar.getManifest();
        }

        @Override
        public CodeSigner[] verifyAndGetSigners(final String cname, final byte[] bytes) {
            return jar.verifyAndGetSigners(cname, bytes);
        }
    }
}
