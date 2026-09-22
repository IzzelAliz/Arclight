package io.izzel.arclight.boot.neoforge.mod;

import net.neoforged.fml.jarcontents.JarContents;
import net.neoforged.fml.jarmoduleinfo.JarModuleInfo;
import net.neoforged.fml.loading.moddiscovery.ModFile;
import net.neoforged.fml.loading.moddiscovery.ModFileParser;
import net.neoforged.fml.jarcontents.CompositeJarContents;
import java.util.List;
import net.neoforged.fml.loading.moddiscovery.readers.JarModsDotTomlModFileReader;
import net.neoforged.neoforgespi.locating.IModFile;
import net.neoforged.neoforgespi.locating.IModFileReader;
import net.neoforged.neoforgespi.locating.ModFileDiscoveryAttributes;

import java.lang.module.ModuleDescriptor;

public class ArclightModFileReader implements IModFileReader {
    @Override
    public IModFile read(JarContents contents, ModFileDiscoveryAttributes attributes) {
        // Never strip real mods or game content. Their mixins and mod entrypoints
        // must remain on the transforming loader, including shaded mod packages.
        if (contents.containsFile(JarModsDotTomlModFileReader.MODS_TOML)) {
            if (!"Arclight".equals(contents.getManifest().getMainAttributes().getValue("Implementation-Title"))) return null;
            final ModFile[] file = new ModFile[1];
            var metadata = new JarModuleInfo() {
                public String name() { return file[0].getId(); }
                public String version() { return file[0].getModFileInfo().versionString(); }
                public ModuleDescriptor createDescriptor(JarContents jar) {
                    var builder = ModuleDescriptor.newAutomaticModule(name()).version(version());
                    // JLine nests its own properties under services/. They are not Java SPI.
                    // Filter only the descriptor scan, leaving runtime resources untouched.
                    var scan = new CompositeJarContents(List.of(jar), List.of(path ->
                        !path.startsWith("META-INF/services/") || path.indexOf('/', "META-INF/services/".length()) < 0));
                    JarModuleInfo.scanAutomaticModule(scan, builder, "assets", "data");
                    file[0].getModFileInfo().usesServices().forEach(builder::uses);
                    return builder.build();
                }
            };
            file[0] = new ModFile(contents, metadata, ModFileParser::modsTomlParser, attributes.withReader(this));
            return file[0];
        }
        // FML 10 loads LIBRARY jars on its early URLClassLoader, which requires
        // original content roots. Parent-first delegation replaces the old JPMS
        // package masking; leave these jars and their SPI resources to FML.
        return null;
    }

    @Override
    public int getPriority() {
        return HIGHEST_SYSTEM_PRIORITY;
    }
}
