package io.izzel.arclight.boot.forge.mod;

import cpw.mods.jarhandling.JarMetadata;
import cpw.mods.jarhandling.SecureJar;
import cpw.mods.jarhandling.impl.SimpleJarMetadata;
import net.minecraftforge.fml.loading.moddiscovery.AbstractModProvider;
import net.minecraftforge.fml.loading.moddiscovery.ModFile;
import net.minecraftforge.fml.loading.moddiscovery.ModFileParser;
import net.minecraftforge.forgespi.locating.IModFile;
import net.minecraftforge.forgespi.locating.IModLocator;

import java.nio.file.Paths;
import java.util.List;

public class ArclightLocator_Forge extends AbstractModProvider implements IModLocator {

    private final IModFile arclight;

    public ArclightLocator_Forge() {
        ModBootstrap.run();
        this.arclight = loadJar();
    }

    @Override
    public List<ModFileOrException> scanMods() {
        return List.of(new ModFileOrException(arclight, null));
    }

    @Override
    public String name() {
        return "arclight";
    }

    protected IModFile loadJar() {
        try {
            var version = System.getProperty("arclight.version");
            if (version == null) {
                throw new IllegalStateException("Arclight bootloader is not initialized");
            }
            var path = Paths.get(".arclight", "mod_file", version + ".jar");
            return new ModFile(SecureJar.from(it -> versionMetadata(it, version), path), this, ModFileParser::modsTomlParser);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private JarMetadata versionMetadata(SecureJar secureJar, String version) {
        return new SimpleJarMetadata("arclight", version.substring(version.indexOf('-') + 1), secureJar.getPackages(), secureJar.getProviders());
    }
}
