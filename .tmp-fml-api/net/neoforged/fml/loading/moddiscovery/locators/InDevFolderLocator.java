/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.loading.moddiscovery.locators;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import net.neoforged.fml.ModLoadingIssue;
import net.neoforged.fml.jarcontents.JarContents;
import net.neoforged.fml.loading.LogMarkers;
import net.neoforged.neoforgespi.ILaunchContext;
import net.neoforged.neoforgespi.locating.IDiscoveryPipeline;
import net.neoforged.neoforgespi.locating.IModFileCandidateLocator;
import net.neoforged.neoforgespi.locating.IncompatibleFileReporting;
import net.neoforged.neoforgespi.locating.ModFileDiscoveryAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This locator finds mods and services that are passed as exploded folders on the classpath and are grouped explicitly.
 */
public class InDevFolderLocator implements IModFileCandidateLocator {
    private static final Logger LOGGER = LoggerFactory.getLogger(InDevFolderLocator.class);

    private final Map<File, VirtualJarManifestEntry> virtualJarMemberIndex = new HashMap<>();

    record VirtualJarManifestEntry(String name, List<File> files) {}

    @Override
    public void findCandidates(ILaunchContext context, IDiscoveryPipeline pipeline) {
        loadFromSystemProperty();

        for (var entry : new HashSet<>(virtualJarMemberIndex.values())) {
            var paths = entry.files.stream().map(File::toPath).toList();
            if (paths.stream().noneMatch(context::isLocated)) {
                try {
                    pipeline.addJarContent(JarContents.ofPaths(paths), ModFileDiscoveryAttributes.DEFAULT, IncompatibleFileReporting.ERROR);
                } catch (IOException e) {
                    pipeline.addIssue(ModLoadingIssue.error("fml.modloadingissue.brokenfile.invalidzip").withAffectedPath(paths.getFirst()).withCause(e));
                }
            }
        }
    }

    private void loadFromSystemProperty() {
        var modFolders = Optional.ofNullable(System.getenv("MOD_CLASSES"))
                .orElse(System.getProperty("fml.modFolders", ""));
        if (!modFolders.isEmpty()) {
            LOGGER.info(LogMarkers.CORE, "Got mod coordinates {} from env", modFolders);
            // "a/b/;c/d/;" ->"modid%%c:\fish\pepper;modid%%c:\fish2\pepper2\;modid2%%c:\fishy\bums;modid2%%c:\hmm"
            var groupedEntries = Arrays.stream(modFolders.split(File.pathSeparator))
                    .collect(Collectors.groupingBy(
                            inp -> {
                                var splitIdx = inp.indexOf("%%");
                                if (splitIdx != -1) {
                                    return inp.substring(0, splitIdx);
                                } else {
                                    return "defaultmodid";
                                }
                            },
                            Collectors.mapping(inp -> {
                                var splitIdx = inp.indexOf("%%");
                                if (splitIdx != -1) {
                                    inp = inp.substring(splitIdx + "%%".length());
                                }
                                return new File(inp);
                            }, Collectors.toList())));
            for (var group : groupedEntries.entrySet()) {
                var virtualJar = new VirtualJarManifestEntry(
                        group.getKey(),
                        group.getValue());
                for (var file : group.getValue()) {
                    virtualJarMemberIndex.put(file, virtualJar);
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
        return "indevfolder";
    }
}
