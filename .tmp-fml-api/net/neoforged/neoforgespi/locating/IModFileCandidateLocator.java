/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforgespi.locating;

import java.io.File;
import net.neoforged.fml.loading.moddiscovery.locators.ModsFolderLocator;
import net.neoforged.neoforgespi.ILaunchContext;

/**
 * Loaded as a ServiceLoader. Takes mechanisms for locating candidate "mod" JARs.
 */
public interface IModFileCandidateLocator extends IOrderedProvider {
    /**
     * Creates an IModFileCandidateLocator that searches for mod jar-files in the given filesystem location.
     */
    static IModFileCandidateLocator forFolder(File folder, String identifier) {
        return new ModsFolderLocator(folder.toPath(), identifier);
    }

    /**
     * Discovers potential mods to be loaded by FML.
     *
     * @param pipeline Adds discovered mods and issues to this pipeline.
     */
    void findCandidates(ILaunchContext context, IDiscoveryPipeline pipeline);
}
