/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.jarmoduleinfo;

import java.lang.module.ModuleDescriptor;
import net.neoforged.fml.jarcontents.JarContents;
import org.jetbrains.annotations.Nullable;

/**
 * {@link JarModuleInfo} implementation for a non-modular jar, turning it into an automatic module.
 */
class AuomaticModuleJarModuleInfo implements JarModuleInfo {
    private final String name;
    private final String version;

    private AuomaticModuleJarModuleInfo(String name, String version) {
        this.name = name;
        this.version = version;
    }

    public static AuomaticModuleJarModuleInfo from(JarContents contents) {
        var nav = ModuleDescriptorFactory.computeNameAndVersion(contents.getPrimaryPath());
        String name = nav.name();
        String version = nav.version();

        String automaticModuleName = contents.getManifest().getMainAttributes().getValue("Automatic-Module-Name");
        if (automaticModuleName != null) {
            name = automaticModuleName;
        }

        return new AuomaticModuleJarModuleInfo(name, version);
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    @Nullable
    public String version() {
        return version;
    }

    @Override
    public ModuleDescriptor createDescriptor(JarContents contents) {
        var bld = ModuleDescriptor.newAutomaticModule(name());
        if (version() != null) {
            bld.version(version());
        }

        ModuleDescriptorFactory.scanAutomaticModule(contents, bld);

        return bld.build();
    }
}
