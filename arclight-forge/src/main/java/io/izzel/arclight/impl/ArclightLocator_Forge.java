package io.izzel.arclight.impl;

import io.izzel.arclight.common.mod.ArclightLocator;
import net.minecraftforge.fml.loading.moddiscovery.ModFile;
import net.minecraftforge.forgespi.locating.IModFile;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.stream.Stream;

public class ArclightLocator_Forge extends ArclightLocator {

    @Override
    protected IModFile loadJars() {
        try {
            return ModFile.newFMLInstance(this, new File(ArclightLocator.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toPath());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
