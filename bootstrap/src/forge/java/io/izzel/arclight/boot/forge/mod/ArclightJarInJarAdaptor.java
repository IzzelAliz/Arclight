package io.izzel.arclight.boot.forge.mod;

import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.moddiscovery.JarInJarDependencyLocator;
import net.minecraftforge.fml.loading.moddiscovery.ModDiscoverer;
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

    @Override
    public List<IModFile> scanMods(Iterable<IModFile> loadedMods) {
        return delegate.scanMods(loadedMods).stream().filter(it -> {
            var optional = getClass().getModule().getLayer().findModule(it.getModFileInfo().moduleName());
            optional.ifPresent(module -> LOGGER.info("Skip jij dependency {}@{} because Arclight has {}",
                it.getModFileInfo().moduleName(), it.getModFileInfo().versionString(), module.getDescriptor().toNameAndVersion()));
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

    @SuppressWarnings("unchecked")
    static void inject() {
        try {
            var field = FMLLoader.class.getDeclaredField("modDiscoverer");
            field.setAccessible(true);
            var discoverer = (ModDiscoverer) field.get(null);
            var locatorField = ModDiscoverer.class.getDeclaredField("dependencyLocatorList");
            locatorField.setAccessible(true);
            var locatorList = (List<IDependencyLocator>) locatorField.get(discoverer);
            var newList = locatorList.stream().map(it -> {
                if (it instanceof JarInJarDependencyLocator) {
                    return new ArclightJarInJarAdaptor(it);
                } else {
                    return it;
                }
            }).toList();
            locatorField.set(discoverer, newList);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
