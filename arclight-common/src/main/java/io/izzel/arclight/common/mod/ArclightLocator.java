package io.izzel.arclight.common.mod;

import com.google.common.collect.ImmutableList;
import net.minecraftforge.fml.loading.moddiscovery.AbstractJarFileLocator;
import net.minecraftforge.fml.loading.moddiscovery.ModFile;
import net.minecraftforge.forgespi.locating.IModFile;

import java.io.File;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

public class ArclightLocator extends AbstractJarFileLocator {

    private final IModFile arclight;

    public ArclightLocator() {
        try {
            this.arclight = new ModFile(new File(ArclightLocator.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toPath(), this);
            this.modJars.put(arclight, createFileSystem(arclight));
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public List<IModFile> scanMods() {
        return ImmutableList.of(arclight);
    }

    @Override
    public String name() {
        return "arclight";
    }

    @Override
    public void initArguments(Map<String, ?> arguments) {
    }
}
