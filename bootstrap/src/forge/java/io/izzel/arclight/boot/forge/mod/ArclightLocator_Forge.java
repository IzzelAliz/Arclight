package io.izzel.arclight.boot.forge.mod;

import cpw.mods.jarhandling.JarMetadata;
import cpw.mods.jarhandling.SecureJar;
import cpw.mods.jarhandling.impl.SimpleJarMetadata;
import net.minecraftforge.forgespi.language.IModFileInfo;
import net.minecraftforge.forgespi.locating.IModFile;
import net.minecraftforge.forgespi.locating.IModLocator;
import net.minecraftforge.forgespi.locating.IModProvider;
import net.minecraftforge.forgespi.locating.ModFileFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
        ModBootstrap.run();
        this.arclight = loadJar();
    }

    @Override
    public List<ModFileOrException> scanMods() {
        ArclightJarInJarAdaptor.inject();
        return List.of(new ModFileOrException(arclight, null));
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
            var version = System.getProperty("arclight.version");
            if (version == null) {
                throw new IllegalStateException("Arclight bootloader is not initialized");
            }
            var cl = forName("net.minecraftforge.fml.loading.moddiscovery.ModFile");
            var lookup = MethodHandles.lookup();
            var handle = lookup.findConstructor(cl, MethodType.methodType(void.class, SecureJar.class, IModProvider.class, ModFileFactory.ModFileInfoParser.class));
            var path = Paths.get(".arclight", "mod_file", version + ".jar");
            var parserCl = forName("net.minecraftforge.fml.loading.moddiscovery.ModFileParser");
            var modsToml = lookup.findStatic(parserCl, "modsTomlParser", MethodType.methodType(IModFileInfo.class, IModFile.class));
            ModFileFactory.ModFileInfoParser parser = modFile -> {
                try {
                    return (IModFileInfo) modsToml.invoke(modFile);
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            };
            return (IModFile) handle.invoke(SecureJar.from(it -> versionMetadata(it, version), path), this, parser);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private JarMetadata versionMetadata(SecureJar secureJar, String version) {
        return new SimpleJarMetadata("arclight", version.substring(version.indexOf('-') + 1), secureJar.getPackages(), List.of());
    }
}
