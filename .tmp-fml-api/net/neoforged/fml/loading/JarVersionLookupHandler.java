/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.loading;

import java.util.Optional;

/**
 * Finds Version data from a package, with possible default values
 */
public class JarVersionLookupHandler {
    public static Optional<String> getVersion(Class<?> clazz) {
        if (clazz.getModule() != null && clazz.getModule().getName() != null) {
            // Named modules can be versioned directly via their jar file.
            if (clazz.getModule().getDescriptor() != null) {
                var version = clazz.getModule().getDescriptor().rawVersion();
                if (version.isPresent()) {
                    return version;
                }
            }
        }

        // When loaded through a non-modular class-loader, we do get the implementation version from the manifest
        if (clazz.getPackage() != null && clazz.getPackage().getImplementationVersion() != null) {
            return Optional.of(clazz.getPackage().getImplementationVersion());
        }

        return Optional.empty();
    }
}
