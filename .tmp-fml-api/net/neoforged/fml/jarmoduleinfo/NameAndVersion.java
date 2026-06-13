/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.jarmoduleinfo;

import org.jetbrains.annotations.Nullable;

/**
 * Holder class for name and version of a module, used in {@link JarModuleInfo} computations.
 * Unfortunately, interfaces cannot hold private records (yet?).
 */
record NameAndVersion(String name, @Nullable String version) {}
