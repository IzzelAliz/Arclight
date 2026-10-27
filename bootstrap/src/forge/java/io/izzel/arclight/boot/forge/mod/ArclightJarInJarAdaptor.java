package io.izzel.arclight.boot.forge.mod;

import net.minecraftforge.fml.loading.moddiscovery.JarInJarDependencyLocator;
import net.minecraftforge.forgespi.locating.IDependencyLocator;
import net.minecraftforge.forgespi.locating.IModFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class ArclightJarInJarAdaptor implements IDependencyLocator {

    private static final Logger LOGGER = LoggerFactory.getLogger("ArclightJiJ");

    private final IDependencyLocator delegate;

    public ArclightJarInJarAdaptor(IDependencyLocator delegate) {
        this.delegate = delegate;
    }

    // Forge 64 discovers dependency locators in a local list, not on a discoverer instance.
    public static List<IModFile> scanMods(IDependencyLocator locator, Iterable<IModFile> loadedMods) {
        return locator instanceof JarInJarDependencyLocator
            ? new ArclightJarInJarAdaptor(locator).scanMods(loadedMods)
            : locator.scanMods(loadedMods);
    }

    @Override
    public List<IModFile> scanMods(Iterable<IModFile> loadedMods) {
        return delegate.scanMods(loadedMods).stream().filter(it -> {
            var optional = getClass().getModule().getLayer().findModule(it.getSecureJar().name());
            optional.ifPresent(module -> LOGGER.info("Skip jij dependency {}@{} because Arclight has {}",
                it.getSecureJar().name(), it.getModFileInfo().versionString(), module.getDescriptor().toNameAndVersion()));
            return optional.isEmpty();
        }).toList();
    }

    @Override
    public String name() {
        return "arclight_jij";
    }

    @Override
    public void scanFile(IModFile modFile, Consumer<Path> pathConsumer) {
        delegate.scanFile(modFile, pathConsumer);
    }

    @Override
    public void initArguments(Map<String, ?> arguments) {
        delegate.initArguments(arguments);
    }

    @Override
    public boolean isValid(IModFile modFile) {
        return delegate.isValid(modFile);
    }

}
