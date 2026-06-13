/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.classloading;

import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleReader;
import java.lang.module.ModuleReference;
import java.net.URI;
import net.neoforged.fml.jarcontents.JarContents;

final class JarContentsModuleReference extends ModuleReference {
    private final JarContents contents;

    JarContentsModuleReference(ModuleDescriptor descriptor, JarContents contents) {
        super(descriptor, getModuleLocation(contents));
        this.contents = contents;
    }

    private static URI getModuleLocation(JarContents contents) {
        if (contents.getContentRoots().isEmpty()) {
            return null;
        }
        return contents.getPrimaryPath().toUri();
    }

    @Override
    public ModuleReader open() {
        return new JarContentsModuleReader(contents);
    }
}
