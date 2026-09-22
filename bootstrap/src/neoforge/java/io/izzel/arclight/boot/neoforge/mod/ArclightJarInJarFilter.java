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
            // FML10 still has no public removal API. This is the one intentional
            // internal dependency: DiscoveryPipeline.loadedFiles, verified against 10.0.36.
            // The supplied loadedMods list is a snapshot and cannot remove candidates.
            var field = pipeline.getClass().getDeclaredField("loadedFiles");
            field.setAccessible(true);
            var loadedFiles = (List<ModFile>) field.get(pipeline);
            var iterator = loadedFiles.iterator();
            while (iterator.hasNext()) {
                var file = iterator.next();
                if (file.getDiscoveryAttributes().parent() == null) continue;
                var name = file.getModuleDescriptor().name();
                if (ArclightJarContentsImplFilter.providesModule(name)) {
                    LOGGER.info("Skip jij dependency {} because it is already on the Arclight parent classpath", name);
                    iterator.remove();
                    file.close();
                }
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("FML discovery pipeline changed; cannot remove duplicate Arclight dependencies", e);
        }
    }

    @Override
    public String toString() {
        return "arclight_jij";
    }

    @Override
    public int getPriority() {
        return LOWEST_SYSTEM_PRIORITY;
    }
}
