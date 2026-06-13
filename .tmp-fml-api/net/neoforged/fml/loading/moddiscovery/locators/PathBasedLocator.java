/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.loading.moddiscovery.locators;

import java.nio.file.Path;
import java.util.List;
import net.neoforged.neoforgespi.ILaunchContext;
import net.neoforged.neoforgespi.locating.IDiscoveryPipeline;
import net.neoforged.neoforgespi.locating.IModFileCandidateLocator;
import net.neoforged.neoforgespi.locating.IncompatibleFileReporting;
import net.neoforged.neoforgespi.locating.ModFileDiscoveryAttributes;

/**
 * "Locates" mods from a fixed set of paths.
 */
public record PathBasedLocator(String name, List<Path> paths) implements IModFileCandidateLocator {
    public PathBasedLocator(String name, Path... paths) {
        this(name, List.of(paths));
    }

    @Override
    public void findCandidates(ILaunchContext context, IDiscoveryPipeline pipeline) {
        for (var path : paths) {
            pipeline.addPath(path, ModFileDiscoveryAttributes.DEFAULT, IncompatibleFileReporting.ERROR);
        }
    }

    @Override
    public int getPriority() {
        // Since this locator uses explicitly specified paths, they should not be handled by other locators first
        return HIGHEST_SYSTEM_PRIORITY;
    }
}
