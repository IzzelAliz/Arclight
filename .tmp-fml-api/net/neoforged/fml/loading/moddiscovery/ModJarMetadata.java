/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.loading.moddiscovery;

import java.lang.module.ModuleDescriptor;
import java.util.Objects;
import net.neoforged.fml.jarcontents.JarContents;
import net.neoforged.fml.jarmoduleinfo.JarModuleInfo;
import net.neoforged.neoforgespi.locating.IModFile;

public final class ModJarMetadata implements JarModuleInfo {
    private IModFile modFile;

    public void setModFile(IModFile file) {
        this.modFile = file;
    }

    @Override
    public String name() {
        return modFile.getId();
    }

    @Override
    public String version() {
        return modFile.getModFileInfo().versionString();
    }

    @Override
    public ModuleDescriptor createDescriptor(JarContents contents) {
        var bld = ModuleDescriptor.newAutomaticModule(name())
                .version(version());

        JarModuleInfo.scanAutomaticModule(contents, bld, "assets", "data");

        modFile.getModFileInfo().usesServices().forEach(bld::uses);
        return bld.build();
    }

    public IModFile modFile() {
        return modFile;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (ModJarMetadata) obj;
        return Objects.equals(this.modFile, that.modFile);
    }

    @Override
    public int hashCode() {
        return Objects.hash(modFile);
    }

    @Override
    public String toString() {
        return "ModJarMetadata[" + "modFile=" + modFile + ']';
    }
}
