/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.classloading;

import java.lang.module.ModuleDescriptor;
import net.neoforged.fml.jarcontents.JarContents;
import net.neoforged.fml.jarmoduleinfo.JarModuleInfo;
import org.jetbrains.annotations.VisibleForTesting;

/**
 * Links {@link JarContents} with how that content will be loaded as a JPMS module.
 */
public record JarContentsModule(JarContents contents, ModuleDescriptor moduleDescriptor) {
    @VisibleForTesting
    public JarContentsModule(JarContents contents) {
        this(contents, JarModuleInfo.from(contents).createDescriptor(contents));
    }

    public String moduleName() {
        return moduleDescriptor.name();
    }
}
