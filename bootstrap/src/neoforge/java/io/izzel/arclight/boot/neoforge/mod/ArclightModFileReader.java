package io.izzel.arclight.boot.neoforge.mod;

import cpw.mods.jarhandling.JarContents;
import cpw.mods.jarhandling.impl.JarContentsImpl;
import net.neoforged.neoforgespi.locating.IModFile;
import net.neoforged.neoforgespi.locating.IModFileReader;
import net.neoforged.neoforgespi.locating.IOrderedProvider;
import net.neoforged.neoforgespi.locating.ModFileDiscoveryAttributes;
import org.jetbrains.annotations.Nullable;

public class ArclightModFileReader implements IModFileReader, IOrderedProvider {
    @Override
    public @Nullable IModFile read(JarContents jarContents, ModFileDiscoveryAttributes modFileDiscoveryAttributes) {
        ArclightJarContentsImplFilter.filter((JarContentsImpl) jarContents);
        return null;
    }

    @Override
    public int getPriority() {
        return HIGHEST_SYSTEM_PRIORITY;
    }
}
