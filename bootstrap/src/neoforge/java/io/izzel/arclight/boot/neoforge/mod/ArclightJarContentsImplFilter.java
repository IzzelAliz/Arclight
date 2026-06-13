package io.izzel.arclight.boot.neoforge.mod;

import io.izzel.arclight.api.Unsafe;
import net.neoforged.fml.jarcontents.JarContents;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Set;

/**
 * Filter out packages already provided by Arclight.
 * FML 26.1 uses {@link net.neoforged.fml.jarcontents.JarContents}; legacy securejarhandler
 * filtering is optional and loaded reflectively when present.
 */
public class ArclightJarContentsImplFilter {
    private static final MethodHandles.Lookup LOOKUP = Unsafe.lookup();
    private static final String LEGACY_IMPL = "cpw.mods.jarhandling.impl.JarContentsImpl";
    private static Class<?> legacyImplClass;
    private static VarHandle PACKAGES;
    private static Set<String> serviceLayerPackages;
    private static final Logger LOGGER = LogManager.getLogger("Arclight");

    static {
        try {
            legacyImplClass = Class.forName(LEGACY_IMPL);
            PACKAGES = LOOKUP.findVarHandle(legacyImplClass, "packages", Set.class);
        } catch (ReflectiveOperationException e) {
            LOGGER.debug("SecureJar JarContentsImpl package filter unavailable (expected on FML 26.1+)");
        }
        // getLayer() is null during early FML discovery; module packages are sufficient for JiJ dedup.
        serviceLayerPackages = ArclightJarContentsImplFilter.class.getModule().getPackages();
    }

    public static void filter(JarContents jar) {
        if (legacyImplClass != null && legacyImplClass.isInstance(jar)) {
            filterLegacy(jar);
        }
        // FML 26.1 JarContents has no mutable package cache; JiJ dedup handles shaded mods.
    }

    private static void filterLegacy(JarContents impl) {
        if (PACKAGES != null) {
            Set<String> raw = (Set<String>) PACKAGES.get(impl);
            Set<String> result = raw.stream()
                    .filter(ArclightJarContentsImplFilter::test)
                    .collect(java.util.stream.Collectors.toSet());
            PACKAGES.set(impl, result);
        }
    }

    public static boolean test(String pkg) {
        return !serviceLayerPackages.contains(pkg);
    }
}
