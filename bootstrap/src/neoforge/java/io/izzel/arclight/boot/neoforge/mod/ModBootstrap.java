package io.izzel.arclight.boot.neoforge.mod;

import io.izzel.arclight.api.ArclightPlatform;
import io.izzel.arclight.api.Unsafe;
import io.izzel.arclight.boot.AbstractBootstrap;
import io.izzel.arclight.installer.ForgeInstaller;
import io.izzel.arclight.installer.MinecraftProvider;
import net.neoforged.fml.classloading.ModuleClassLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.MarkerManager;

import java.io.File;
import java.lang.ModuleLayer;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Arclight bootstrap for FML 11+.
 * <p>
 * FML 26.1 replaces {@code cpw.mods.cl.ModuleClassLoader} with
 * {@link net.neoforged.fml.classloading.ModuleClassLoader}. Arclight libraries are added via
 * {@code legacyClassPath} ({@link io.izzel.arclight.installer.NeoforgeInstaller}) rather than
 * mutating the game-layer classloader configuration at discovery time.
 */
public class ModBootstrap implements AbstractBootstrap {

    private static ClassLoader bootstrapParent;
    private static boolean bootstrapped;

    static void run() {
        if (bootstrapped) return;
        bootstrapped = true;
        var logger = LogManager.getLogger("Arclight");
        var marker = MarkerManager.getMarker("INSTALL");
        try {
            List<Path> libraries = MinecraftProvider.modInstall(s -> logger.info(marker, s));
            addLibrariesToClasspath(libraries);
            bootstrapParent = ModBootstrap.class.getClassLoader();
            new ModBootstrap().inject();
        } catch (Throwable e) {
            logger.error("Error bootstrap Arclight", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * After FML builds the transforming game classloader, delegate bootstrap-layer packages to the
     * Arclight parent loader so shaded JiJ libraries do not shadow boot classes.
     */
    @SuppressWarnings("unchecked")
    public static void postRun() {
        if (bootstrapParent == null) return;
        try {
            var classLoader = Thread.currentThread().getContextClassLoader();
            if (!(classLoader instanceof ModuleClassLoader moduleClassLoader)) {
                bootstrapParent = null;
                return;
            }
            var parentField = ModuleClassLoader.class.getDeclaredField("parentLoaders");
            var parentLoaders = (Map<String, ClassLoader>) Unsafe.getObject(
                    moduleClassLoader, Unsafe.objectFieldOffset(parentField));
            var parent = bootstrapParent;
            for (var pk : ModBootstrap.class.getModule().getPackages()) {
                parentLoaders.put(pk, parent);
            }
            for (var module : ModuleLayer.boot().modules()) {
                if (module.getClassLoader() == parent) {
                    for (var pk : module.getPackages()) {
                        parentLoaders.put(pk, parent);
                    }
                }
            }
            bootstrapParent = null;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    private static void addLibrariesToClasspath(List<Path> libraries) throws Throwable {
        for (Path library : libraries) {
            ForgeInstaller.addToPath(library);
        }
    }

    private void inject() throws Throwable {
        dirtyHacks();
        setupMod(ArclightPlatform.NEOFORGE);
        injectClassPath();
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
}
