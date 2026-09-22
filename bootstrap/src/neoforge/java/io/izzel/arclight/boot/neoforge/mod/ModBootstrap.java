package io.izzel.arclight.boot.neoforge.mod;

import io.izzel.arclight.api.ArclightPlatform;
import io.izzel.arclight.api.Unsafe;
import io.izzel.arclight.boot.AbstractBootstrap;
import io.izzel.arclight.installer.MinecraftProvider;
import net.neoforged.fml.util.ClasspathResourceUtils;
import org.apache.logging.log4j.LogManager;

import java.lang.invoke.MethodType;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class ModBootstrap implements AbstractBootstrap {

    private static boolean initialized;

    static synchronized void run() {
        if (initialized) return;
        var logger = LogManager.getLogger("Arclight");
        try {
            // Standalone installs already expose these jars on the FML parent loader.
            // When installed as an early-service mod, append them to that service's
            // URLClassLoader. FML10's game loader delegates to it without JPMS surgery.
            loadLibraries(MinecraftProvider.modInstall(logger::info));
            var bootstrap = new ModBootstrap();
            bootstrap.setupMod(ArclightPlatform.NEOFORGE);
            bootstrap.dirtyHacks();
            initialized = true;
        } catch (Throwable e) {
            throw new IllegalStateException("Error bootstrapping Arclight for FML 10", e);
        }
    }

    private static void loadLibraries(List<Path> paths) throws Throwable {
        var loader = ModBootstrap.class.getClassLoader();
        var present = ClasspathResourceUtils.getAllClasspathItems(loader).stream()
            .map(path -> path.toAbsolutePath().normalize()).collect(Collectors.toSet());
        var missing = paths.stream().map(path -> path.toAbsolutePath().normalize())
            .filter(path -> !present.contains(path)).toList();
        if (missing.isEmpty()) return;
        if (!(loader instanceof URLClassLoader)) {
            throw new IllegalStateException("Arclight libraries are missing from the FML parent classpath: " + missing);
        }
        var addUrl = Unsafe.lookup().findVirtual(URLClassLoader.class, "addURL", MethodType.methodType(void.class, URL.class));
        for (var path : missing) {
            addUrl.invoke(loader, path.toUri().toURL());
        }
    }
}
