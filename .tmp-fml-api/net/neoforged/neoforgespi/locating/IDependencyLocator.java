/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforgespi.locating;

import java.util.List;

/**
 * Loaded as a ServiceLoader. Takes mechanisms for locating candidate "mod-dependencies".
 * and transforms them into {@link IModFile} objects.
 */
public interface IDependencyLocator extends IOrderedProvider {
    /**
     * Invoked to find all mod dependencies that this dependency locator can find.
     * It is not guaranteed that all these are loaded into the runtime,
     * as such the result of this method should be seen as a list of candidates to load.
     */
    void scanMods(List<IModFile> loadedMods, IDiscoveryPipeline pipeline);
}
