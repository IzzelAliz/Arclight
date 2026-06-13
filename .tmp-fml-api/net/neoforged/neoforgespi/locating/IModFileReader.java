/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforgespi.locating;

import net.neoforged.fml.jarcontents.JarContents;
import org.jetbrains.annotations.Nullable;

/**
 * Inspects {@link JarContents} found by {@link IModFileCandidateLocator} and tries to turn them into {@link IModFile}.
 * <p>
 * Picked up via ServiceLoader.
 */
public interface IModFileReader extends IOrderedProvider {
    /**
     * Provides a mod from the given {@code jar}.
     * <p>
     * Throwing {@link net.neoforged.fml.ModLoadingException} will report contained issues as the reason for
     * incompatibility to players, unless another reader successfully reads the jar.
     * <p>
     * Other thrown exceptions will be reported as errors and cause loading to fail.
     *
     * @param jar        the mod jar contents
     * @param attributes The attributes relating to this mod files discovery.
     * @return {@code null} if this provider can't handle the given jar,
     *         otherwise the mod-file created from the given contents and attributes.
     */
    @Nullable
    IModFile read(JarContents jar, ModFileDiscoveryAttributes attributes);
}
