package io.izzel.arclight.boot.neoforge.mod;

import net.neoforged.fml.loading.moddiscovery.ModFile;
import net.neoforged.neoforgespi.locating.IDependencyLocator;
import net.neoforged.neoforgespi.locating.IDiscoveryPipeline;
import net.neoforged.neoforgespi.locating.IModFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ArclightJarInJarFilter implements IDependencyLocator {

    private static final Logger LOGGER = LoggerFactory.getLogger("ArclightJiJ");

    @SuppressWarnings("unchecked")
    @Override
    public void scanMods(List<IModFile> loadedMods, IDiscoveryPipeline pipeline) {
        try {
            var field = pipeline.getClass().getDeclaredField("loadedFiles");
            field.setAccessible(true);
            var loadedFiles = (List<ModFile>) field.get(pipeline);
            loadedFiles.removeIf(it -> {
                var optional = getClass().getModule().getLayer().findModule(it.getModFileInfo().moduleName());
                optional.ifPresent(module -> LOGGER.info("Skip jij dependency {}@{} because Arclight has {}",
                    it.getModFileInfo().moduleName(), it.getModFileInfo().versionString(), module.getDescriptor().toNameAndVersion()));
                return optional.isPresent();
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        return "arclight_jij";
    }
}
