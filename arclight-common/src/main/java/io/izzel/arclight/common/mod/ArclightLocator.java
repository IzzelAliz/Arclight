package io.izzel.arclight.common.mod;

import com.google.common.collect.ImmutableList;
import net.minecraftforge.fml.loading.moddiscovery.AbstractJarFileLocator;
import net.minecraftforge.forgespi.locating.IModFile;

import java.util.List;
import java.util.Map;

public abstract class ArclightLocator extends AbstractJarFileLocator {

    private final IModFile arclight;

    public ArclightLocator() {
        this.arclight = loadJars();
        this.modJars.put(arclight, createFileSystem(arclight));
    }

    protected abstract IModFile loadJars();

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
