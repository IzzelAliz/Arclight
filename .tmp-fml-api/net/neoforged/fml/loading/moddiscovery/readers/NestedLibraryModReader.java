/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.loading.moddiscovery.readers;

import net.neoforged.fml.jarcontents.JarContents;
import net.neoforged.neoforgespi.locating.IModFile;
import net.neoforged.neoforgespi.locating.IModFileReader;
import net.neoforged.neoforgespi.locating.ModFileDiscoveryAttributes;
import org.jetbrains.annotations.Nullable;

/**
 * This reader will essentially handle <strong>all</strong> files as plain Java libraries,
 * but will only do so for candidates that are embedded in recognized mod files.
 * <p/>
 * If a plain jar-file (that is not a mod or markes as a library) is present on the classpath
 * or in the mods folder, this is usually a mistake.
 * However, if such a file is embedded in a mod jar, it is usually a deliberate decision by the modder.
 */
public class NestedLibraryModReader implements IModFileReader {
    @Override
    public @Nullable IModFile read(JarContents jar, ModFileDiscoveryAttributes discoveryAttributes) {
        // We only consider jars that are contained in the context of another mod valid library targets,
        // since we assume those have been included deliberately. Loose jar files in the mods directory
        // are not considered, since those are likely to have been dropped in by accident.
        if (discoveryAttributes.parent() != null) {
            return IModFile.create(jar, JarModsDotTomlModFileReader::manifestParser, IModFile.Type.LIBRARY, discoveryAttributes);
        }

        return null;
    }

    @Override
    public String toString() {
        return "nested library mod provider";
    }

    @Override
    public int getPriority() {
        // Since this will capture *any* nested jar as a library it should always run last
        return LOWEST_SYSTEM_PRIORITY;
    }
}
