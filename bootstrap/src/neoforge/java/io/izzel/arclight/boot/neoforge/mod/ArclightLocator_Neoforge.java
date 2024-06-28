package io.izzel.arclight.boot.neoforge.mod;

import net.neoforged.neoforgespi.ILaunchContext;
import net.neoforged.neoforgespi.locating.IDiscoveryPipeline;
import net.neoforged.neoforgespi.locating.IModFileCandidateLocator;
import net.neoforged.neoforgespi.locating.IncompatibleFileReporting;
import net.neoforged.neoforgespi.locating.ModFileDiscoveryAttributes;

import java.nio.file.Path;
import java.nio.file.Paths;

public class ArclightLocator_Neoforge implements IModFileCandidateLocator {

    private final Path arclight;

    public ArclightLocator_Neoforge() {
        ModBootstrap.run();
        this.arclight = loadJar();
    }

    protected Path loadJar() {
        var version = System.getProperty("arclight.version");
        return Paths.get(".arclight", "mod_file", version + ".jar");
    }

    @Override
    public void findCandidates(ILaunchContext context, IDiscoveryPipeline pipeline) {
        pipeline.addPath(this.arclight, ModFileDiscoveryAttributes.DEFAULT, IncompatibleFileReporting.WARN_ALWAYS);
    }

    @Override
    public String toString() {
        return "arclight";
    }
}
