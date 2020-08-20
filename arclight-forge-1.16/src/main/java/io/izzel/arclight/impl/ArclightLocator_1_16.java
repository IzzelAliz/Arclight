package io.izzel.arclight.impl;

import io.izzel.arclight.common.mod.ArclightLocator;
import net.minecraftforge.fml.loading.moddiscovery.ModFile;
import net.minecraftforge.forgespi.locating.IModFile;

import java.io.File;
import java.net.URISyntaxException;

public class ArclightLocator_1_16 extends ArclightLocator {

    @Override
    protected IModFile loadJars() {
        try {
            return ModFile.newFMLInstance(new File(ArclightLocator.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toPath(), this);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
