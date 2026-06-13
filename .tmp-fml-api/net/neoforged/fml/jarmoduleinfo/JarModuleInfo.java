/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.jarmoduleinfo;

import java.lang.module.ModuleDescriptor;
import java.util.Set;
import net.neoforged.fml.jarcontents.JarContents;
import org.jetbrains.annotations.Nullable;

/**
 * Describes the modular properties of a Jar file, with direct access to the module name and version,
 * as well as the ability to create a module descriptor for a given {@link JarContents}, which may
 * involve scanning the Jar file for packages.
 */
public interface JarModuleInfo {
    String name();

    @Nullable
    String version();

    ModuleDescriptor createDescriptor(JarContents contents);

    /**
     * Builds the jar metadata for a jar following the normal rules for Java jars.
     *
     * <p>If the jar has a {@code module-info.class} file, the module info is read from there.
     * Otherwise, the jar is an automatic module, whose name is optionally derived
     * from {@code Automatic-Module-Name} in the manifest.
     */
    static JarModuleInfo from(JarContents jar) {
        var moduleInfoResource = jar.get("module-info.class");
        if (moduleInfoResource != null) {
            return new ModuleJarModuleInfo(moduleInfoResource);
        } else {
            return AuomaticModuleJarModuleInfo.from(jar);
        }
    }

    /**
     * Scans an automatic module for the following and applies them to the given module builder.
     *
     * <ul>
     * <li>Packages containing class files.</li>
     * <li>Java {@link java.util.ServiceLoader} providers found in {@code META-INF/services/}</li>
     * </ul>
     *
     * @param excludedRootDirectories Allows for additional root directories to be completely ignored for scanning
     *                                packages. Useful if it is known beforehand that certain subdirectories
     *                                are unlikely to contain classes.
     */
    static void scanAutomaticModule(
            JarContents jar,
            ModuleDescriptor.Builder builder,
            String... excludedRootDirectories) {
        ModuleDescriptorFactory.scanAutomaticModule(jar, builder, excludedRootDirectories);
    }

    /**
     * Scans a given Jar for all packages that contain files for use with {@link ModuleDescriptor#read}.
     * <p>Unlike {@link #scanAutomaticModule}, this also finds packages that contain only resource files, which is
     * consistent with the behavior of {@link java.lang.module.ModuleFinder} for modular Jar files.
     */
    static Set<String> scanModulePackages(JarContents jar) {
        return ModuleDescriptorFactory.scanModulePackages(jar);
    }
}
