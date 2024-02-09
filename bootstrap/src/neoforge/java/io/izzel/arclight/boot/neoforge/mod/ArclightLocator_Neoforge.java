package io.izzel.arclight.boot.neoforge.mod;

import cpw.mods.jarhandling.JarContentsBuilder;
import cpw.mods.jarhandling.SecureJar;
import cpw.mods.jarhandling.impl.SimpleJarMetadata;
import net.neoforged.neoforgespi.language.IModFileInfo;
import net.neoforged.neoforgespi.locating.IModFile;
import net.neoforged.neoforgespi.locating.IModLocator;
import net.neoforged.neoforgespi.locating.IModProvider;
import net.neoforged.neoforgespi.locating.ModFileFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.lang.Class.forName;

public class ArclightLocator_Neoforge implements IModLocator {

    private final IModFile arclight;

    public ArclightLocator_Neoforge() {
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
            var cl = forName("net.neoforged.fml.loading.moddiscovery.ModFile");
            var lookup = MethodHandles.lookup();
            var handle = lookup.findConstructor(cl, MethodType.methodType(void.class, SecureJar.class, IModProvider.class, ModFileFactory.ModFileInfoParser.class));
            var version = System.getProperty("arclight.version");
            var path = Paths.get(".arclight", "mod_file", version + ".jar");
            var parserCl = forName("net.neoforged.fml.loading.moddiscovery.ModFileParser");
            var modsToml = lookup.findStatic(parserCl, "modsTomlParser", MethodType.methodType(IModFileInfo.class, IModFile.class));
            ModFileFactory.ModFileInfoParser parser = modFile -> {
                try {
                    return (IModFileInfo) modsToml.invoke(modFile);
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            };
            return (IModFile) handle.invoke(withVersion(path, version), this, parser);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private SecureJar withVersion(Path path, String version) {
        var contents = new JarContentsBuilder().paths(path).build();
        return SecureJar.from(contents, new SimpleJarMetadata("arclight", version.substring(version.indexOf('-') + 1),
                contents::getPackages, List.of()));
    }
}
