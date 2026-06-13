/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.startup;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import net.neoforged.api.distmarker.Dist;
import org.jetbrains.annotations.Nullable;

/**
 * @param gameDirectory
 * @param headless
 * @param dist                      If set to null, the distribution being launched is auto-detected, otherwise it is set to this.
 *                                  In a dev-environment where a "joined" distribution is being used, this parameter also disables
 *                                  access to classes and resources of the inactive distribution, if {@code cleanDist} is also set.
 * @param cleanDist                 If enabled, the loader will try to prevent loading Minecraft classes that do not belong to {@code dist}, but are
 *                                  otherwise present on the classpath (i.e. in joined distribution scenarios in development).
 * @param programArgs
 * @param claimedFiles
 * @param unclaimedClassPathEntries
 * @param parentClassLoader
 */
public record StartupArgs(
        Path gameDirectory,
        boolean headless,
        @Nullable Dist dist,
        boolean cleanDist,
        String[] programArgs,
        Set<File> claimedFiles,
        List<File> unclaimedClassPathEntries,
        @Nullable ClassLoader parentClassLoader) {}
