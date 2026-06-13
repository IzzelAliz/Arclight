/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforgespi.locating;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import net.neoforged.fml.jarcontents.JarContents;
import net.neoforged.neoforgespi.IIssueReporting;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * Offers services to {@link IModFileCandidateLocator locators} for adding mod files in various stages to the
 * discovery pipeline.
 */
@ApiStatus.NonExtendable
public interface IDiscoveryPipeline extends IIssueReporting {
    /**
     * Adds a single file or folder to the discovery pipeline,
     * to be further processed by registered {@linkplain IModFileReader readers} into a {@linkplain IModFile mod file}.
     *
     * @param path                      The path
     * @param attributes                Additional attributes that describe the circumstance of how this path was discovered.
     * @param incompatibleFileReporting The desired behavior if the given file or folder is deemed to be incompatible with NeoForge.
     * @return The resulting mod file if the file or folder was successfully read.
     */
    default Optional<IModFile> addPath(Path path, ModFileDiscoveryAttributes attributes, IncompatibleFileReporting incompatibleFileReporting) {
        return addPath(List.of(path), attributes, incompatibleFileReporting);
    }

    /**
     * Adds a group of files or folders to the discovery pipeline,
     * to be further processed by registered {@linkplain IModFileReader readers} into a {@linkplain IModFile mod file}.
     *
     * @param groupedPaths              A set of files and folders that are combined into a single virtual Jar file for mod loading.
     * @param attributes                Additional attributes that describe the circumstance of how this path was discovered.
     * @param incompatibleFileReporting The desired behavior if the given file or folder is deemed to be incompatible with NeoForge.
     * @return The resulting mod file if the file or folder was successfully read.
     */
    Optional<IModFile> addPath(List<Path> groupedPaths, ModFileDiscoveryAttributes attributes, IncompatibleFileReporting incompatibleFileReporting);

    /**
     * Adds a pre-created {@link JarContents jar} to the discovery pipeline
     * to be further processed by registered {@linkplain IModFileReader readers} into a {@linkplain IModFile mod file}.
     *
     * @param contents                  The contents of the mod file. The pipeline will take ownership of this and close it when it is no longer used.
     * @param attributes                Additional attributes that describe the circumstance of how this path was discovered.
     * @param incompatibleFileReporting The desired behavior if the given file or folder is deemed to be incompatible with NeoForge.
     * @return The resulting mod file if the file or folder was successfully read.
     */
    Optional<IModFile> addJarContent(JarContents contents, ModFileDiscoveryAttributes attributes, IncompatibleFileReporting incompatibleFileReporting);

    /**
     * Add a pre-created mod file to the discovery pipeline.
     *
     * @param modFile The mod file. This must not be a custom implementation of {@link IModFile}. Use the static factory methods on {@link IModFile} instead.
     * @return True if the file was successfully added.
     */
    boolean addModFile(IModFile modFile);

    /**
     * Use the registered {@linkplain IModFileReader readers} to attempt to create a mod-file from the given jar
     * contents.
     *
     * @param attributes Additional attributes that describe the circumstance of how this path was discovered.
     * @return The created mod file or null if no reader was able to handle the jar contents.
     */
    @Nullable
    IModFile readModFile(JarContents jarContents, ModFileDiscoveryAttributes attributes);
}
