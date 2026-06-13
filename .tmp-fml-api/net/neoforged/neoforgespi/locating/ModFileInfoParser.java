/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforgespi.locating;

import net.neoforged.neoforgespi.language.IModFileInfo;

/**
 * A parser specification for building a particular mod files metadata.
 */
@FunctionalInterface
public interface ModFileInfoParser {
    /**
     * Invoked to get the freshly build mod files metadata.
     *
     * @param file The file to parse the metadata for.
     * @return The mod file metadata info.
     */
    IModFileInfo build(IModFile file) throws InvalidModFileException;
}
