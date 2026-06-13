package io.izzel.arclight.boot.neoforge.mod;

import net.neoforged.fml.jarcontents.JarContents;
import net.neoforged.neoforgespi.locating.IModFile;
import net.neoforged.neoforgespi.locating.IModFileReader;
import net.neoforged.neoforgespi.locating.IOrderedProvider;
import net.neoforged.neoforgespi.locating.ModFileDiscoveryAttributes;
import org.jetbrains.annotations.Nullable;

public class ArclightModFileReader implements IModFileReader, IOrderedProvider {
    @Override
    public @Nullable IModFile read(JarContents jar, ModFileDiscoveryAttributes attributes) {
        ArclightJarContentsImplFilter.filter(jar);
        return null;
    }

    @Override
    public int getPriority() {
        return HIGHEST_SYSTEM_PRIORITY;
    }
}
