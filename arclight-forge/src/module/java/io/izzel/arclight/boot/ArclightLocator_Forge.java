package io.izzel.arclight.boot;

import cpw.mods.jarhandling.JarMetadata;
import cpw.mods.jarhandling.SecureJar;
import cpw.mods.jarhandling.impl.SimpleJarMetadata;
import net.minecraftforge.forgespi.locating.IModFile;
import net.minecraftforge.forgespi.locating.IModLocator;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.lang.Class.forName;

public class ArclightLocator_Forge implements IModLocator {

    private final IModFile arclight;

    public ArclightLocator_Forge() {
        this.arclight = loadJar();
    }

    @Override
    public List<IModFile> scanMods() {
        return List.of(arclight);
    }

    @Override
    public String name() {
        return "arclight";
    }

    @Override
    public void scanFile(IModFile file, Consumer<Path> pathConsumer) {
        final Function<Path, SecureJar.Status> status = p -> file.getSecureJar().verifyPath(p);
        try (Stream<Path> files = Files.find(file.getSecureJar().getRootPath(), Integer.MAX_VALUE, (p, a) -> p.getNameCount() > 0 && p.getFileName().toString().endsWith(".class"))) {
            file.setSecurityStatus(files.peek(pathConsumer).map(status).reduce((s1, s2) -> SecureJar.Status.values()[Math.min(s1.ordinal(), s2.ordinal())]).orElse(SecureJar.Status.INVALID));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void initArguments(Map<String, ?> arguments) {
    }

    @Override
    public boolean isValid(IModFile modFile) {
        return true;
    }

    protected IModFile loadJar() {
        try {
            var module = getClass().getModule();
            for (var m : module.getLayer().modules()) {
                module.addReads(m);
            }
            for (var layer : module.getLayer().parents()) {
                layer.modules().forEach(module::addReads);
            }
            var cl = forName("net.minecraftforge.fml.loading.moddiscovery.ModFile");
            var lookup = MethodHandles.lookup();
            var handle = lookup.findStatic(cl, "newFMLInstance", MethodType.methodType(cl, IModLocator.class, SecureJar.class));
            var path = new File(System.getProperty("arclight.selfPath")).toPath();
            return (IModFile) handle.invoke(this, SecureJar.from(this::excludePackages, path));
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private static final Set<String> EXCLUDES = Set.of(
        "com.google",
        "io.izzel.arclight.boot",
        "javax",
        "org.slf4j",
        "org.apache.logging.slf4j"
    );

    private JarMetadata excludePackages(SecureJar secureJar) {
        secureJar.getPackages().removeIf(it -> EXCLUDES.stream().anyMatch(it::startsWith));
        return new SimpleJarMetadata("arclight", null, secureJar.getPackages(),
            List.of(new SecureJar.Provider(
                "cpw.mods.modlauncher.serviceapi.ILaunchPluginService",
                List.of("io.izzel.arclight.common.asm.ArclightImplementer")
            )));
    }
}
