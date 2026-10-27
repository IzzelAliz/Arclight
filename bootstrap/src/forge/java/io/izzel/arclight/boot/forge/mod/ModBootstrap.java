package io.izzel.arclight.boot.forge.mod;

import cpw.mods.jarhandling.SecureJar;
import cpw.mods.modlauncher.LaunchPluginHandler;
import cpw.mods.modlauncher.Launcher;
import cpw.mods.modlauncher.serviceapi.ILaunchPluginService;
import io.izzel.arclight.api.ArclightPlatform;
import io.izzel.arclight.api.Unsafe;
import io.izzel.arclight.boot.AbstractBootstrap;
import io.izzel.arclight.installer.ForgeInstaller;
import io.izzel.arclight.installer.MinecraftProvider;
import net.minecraftforge.securemodules.SecureModuleClassLoader;
import net.minecraftforge.securemodules.SecureModuleFinder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.MarkerManager;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.io.File;
import java.lang.module.Configuration;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.lang.module.ResolvedModule;
import java.nio.file.Path;
import java.util.*;

public class ModBootstrap implements AbstractBootstrap {

    public record ModBoot(Configuration configuration, ClassLoader parent) {}

    private static ModBoot modBoot;
    private static boolean discovererPatched;

    static void run() {
        var plugin = Launcher.INSTANCE.environment().findLaunchPlugin("arclight_implementer");
        if (plugin.isPresent()) {
            return;
        }
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

    // No need to check again since it's already checked in run()
    @Override
    public void dirtyHacks() throws Exception {
        AbstractBootstrap.super.dirtyHacks();
    }

    static synchronized void patchModDiscoverer(Class<?> bootstrap) throws Exception {
        if (discovererPatched) {
            return;
        }
        final var name = "net/minecraftforge/fml/loading/moddiscovery/ModDiscoverer";
        try (var in = bootstrap.getClassLoader().getResourceAsStream(name + ".class")) {
            if (in == null) {
                throw new IllegalStateException("Cannot find Forge ModDiscoverer");
            }
            var clazz = new ClassNode();
            new ClassReader(in).accept(clazz, 0);
            int patched = 0;
            for (var method : clazz.methods) {
                if (!method.name.equals("discoverMods") || !method.desc.equals("(Ljava/util/Map;)Lnet/minecraftforge/fml/loading/moddiscovery/ModValidator;")) {
                    continue;
                }
                for (var insn : method.instructions) {
                    if (insn instanceof MethodInsnNode call && call.getOpcode() == Opcodes.INVOKEINTERFACE
                        && call.owner.equals("net/minecraftforge/forgespi/locating/IDependencyLocator")
                        && call.name.equals("scanMods") && call.desc.equals("(Ljava/lang/Iterable;)Ljava/util/List;")) {
                        method.instructions.set(call, new MethodInsnNode(Opcodes.INVOKESTATIC,
                            "io/izzel/arclight/boot/forge/mod/ArclightJarInJarAdaptor", "scanMods",
                            "(Lnet/minecraftforge/forgespi/locating/IDependencyLocator;Ljava/lang/Iterable;)Ljava/util/List;", false));
                        patched++;
                    }
                }
            }
            if (patched != 1) {
                throw new IllegalStateException("Cannot adapt Forge ModDiscoverer: expected one dependency scan, found " + patched);
            }
            // The static adapter consumes the same stack values; retain the original frames.
            var cw = new ClassWriter(0);
            clazz.accept(cw);
            byte[] bytes = cw.toByteArray();
            Unsafe.defineClass(name.replace('/', '.'), bytes, 0, bytes.length, bootstrap.getClassLoader(), bootstrap.getProtectionDomain());
            discovererPatched = true;
        }
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
        var modulesField = SecureModuleClassLoader.class.getDeclaredField("ourModules");
        var modules = (Map<String, ModuleReference>) Unsafe.getObject(classLoader, Unsafe.objectFieldOffset(modulesField));
        for (var mod : conf.modules()) {
            for (var pk : mod.reference().descriptor().packages()) {
                packageLookup.put(pk, mod);
            }
            modules.put(mod.name(), mod.reference());
            resolvedRoots.put(mod.name(), mod.reference());
        }
    }
}
