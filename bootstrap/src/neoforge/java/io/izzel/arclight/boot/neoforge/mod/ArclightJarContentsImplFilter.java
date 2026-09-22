package io.izzel.arclight.boot.neoforge.mod;

import net.neoforged.fml.jarcontents.CompositeJarContents;
import net.neoforged.fml.jarcontents.JarContents;
import net.neoforged.fml.jarmoduleinfo.JarModuleInfo;
import net.neoforged.fml.util.ClasspathResourceUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Masks parent-provided library packages using FML10's supported content filters. */
public final class ArclightJarContentsImplFilter {

    private record ParentContent(Set<String> packages, Set<String> modules) {}

    private static final class ParentHolder {
        static final ParentContent CONTENT = scanParentContent();
    }

    private static ParentContent scanParentContent() {
        var packages = new HashSet<String>();
        var modules = new HashSet<String>();
        ModuleLayer.boot().modules().forEach(module -> {
            packages.addAll(module.getPackages());
            modules.add(module.getName());
        });
        // FML10 services live on a plain URL classpath, not in a service ModuleLayer.
        // Scan the service loader (not the TCCL, which later becomes the game loader).
        for (var path : ClasspathResourceUtils.getAllClasspathItems(ArclightJarContentsImplFilter.class.getClassLoader())) {
            if (!Files.exists(path)) continue;
            try (var contents = JarContents.ofPath(path)) {
                var metadata = JarModuleInfo.from(contents);
                modules.add(metadata.name());
                // Classpath jars may have duplicate/invalid JPMS service declarations;
                // scanning packages does not need to build their module descriptors.
                packages.addAll(JarModuleInfo.scanModulePackages(contents));
            } catch (IOException e) {
                throw new UncheckedIOException("Cannot inspect parent classpath " + path, e);
            }
        }
        return new ParentContent(Set.copyOf(packages), Set.copyOf(modules));
    }

    public static boolean providesModule(String name) {
        return ParentHolder.CONTENT.modules().contains(name);
    }

    public static boolean test(String pkg) {
        return !ParentHolder.CONTENT.packages().contains(pkg);
    }

    public static JarContents filter(JarContents contents) {
        return new CompositeJarContents(List.of(contents), List.of(path -> {
            // Rebuild library descriptors from the remaining content. An explicit
            // descriptor may still export/provide packages which were just removed.
            if (path.equals("module-info.class") || path.startsWith("META-INF/versions/") && path.endsWith("/module-info.class")) return false;
            if (path.startsWith("META-INF/")) return !path.startsWith("META-INF/services/");
            int slash = path.lastIndexOf('/');
            return slash < 0 || test(path.substring(0, slash).replace('/', '.'));
        }));
    }
}
