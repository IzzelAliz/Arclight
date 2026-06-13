/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.loading.moddiscovery.locators;

import java.io.IOException;
import java.nio.file.Files;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import net.neoforged.fml.ModLoadingIssue;
import net.neoforged.fml.loading.moddiscovery.ModFile;
import net.neoforged.fml.loading.moddiscovery.readers.JarModsDotTomlModFileReader;
import net.neoforged.fml.util.ClasspathResourceUtils;
import net.neoforged.neoforgespi.ILaunchContext;
import net.neoforged.neoforgespi.locating.IDiscoveryPipeline;
import net.neoforged.neoforgespi.locating.IModFileCandidateLocator;
import net.neoforged.neoforgespi.locating.IncompatibleFileReporting;
import net.neoforged.neoforgespi.locating.ModFileDiscoveryAttributes;

/**
 * This locator finds mods and game libraries that are passed as jar files on the classpath.
 */
public class InDevJarLocator implements IModFileCandidateLocator {
    @Override
    public void findCandidates(ILaunchContext context, IDiscoveryPipeline pipeline) {
        // Search for mods first, since all mods need to be located
        for (var path : ClasspathResourceUtils.findFileSystemRootsOfFileOnClasspath(JarModsDotTomlModFileReader.MODS_TOML)) {
            if (Files.isRegularFile(path)) {
                pipeline.addPath(path, ModFileDiscoveryAttributes.DEFAULT, IncompatibleFileReporting.WARN_ON_KNOWN_INCOMPATIBILITY);
            }
        }

        // For (game)-libraries, we need to inspect the Jar manifests for the mod type attribute
        for (var path : ClasspathResourceUtils.findFileSystemRootsOfFileOnClasspath(JarFile.MANIFEST_NAME)) {
            if (Files.isRegularFile(path)) {
                Manifest manifest;
                // It should be faster to just use JarFile here, since any item that's on the original classpath
                // should have been loaded into a JarFile already. JarFile internally shares state, so this is "free".
                try (var jarFile = new JarFile(path.toFile(), false)) {
                    manifest = jarFile.getManifest();
                } catch (IOException e) {
                    pipeline.addIssue(ModLoadingIssue.error("fml.modloadingissue.brokenfile.invalidzip").withAffectedPath(path).withCause(e));
                    continue;
                }

                var modType = manifest.getMainAttributes().getValue(ModFile.TYPE);
                if (modType != null) {
                    pipeline.addPath(path, ModFileDiscoveryAttributes.DEFAULT, IncompatibleFileReporting.WARN_ON_KNOWN_INCOMPATIBILITY);
                }
            }
        }
    }

    @Override
    public int getPriority() {
        // We need to get the explicitly grouped items out of the way before anyone else claims them
        return HIGHEST_SYSTEM_PRIORITY;
    }

    @Override
    public String toString() {
        return "indevjar";
    }
}
