/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.loading.moddiscovery.locators;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import net.neoforged.fml.ModLoadingException;
import net.neoforged.fml.ModLoadingIssue;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.fml.loading.LogMarkers;
import net.neoforged.fml.loading.StringUtils;
import net.neoforged.neoforgespi.ILaunchContext;
import net.neoforged.neoforgespi.locating.IDiscoveryPipeline;
import net.neoforged.neoforgespi.locating.IModFileCandidateLocator;
import net.neoforged.neoforgespi.locating.IncompatibleFileReporting;
import net.neoforged.neoforgespi.locating.ModFileDiscoveryAttributes;
import org.slf4j.Logger;

/**
 * Support loading mods located in JAR files in the mods folder
 */
public class ModsFolderLocator implements IModFileCandidateLocator {
    private static final String SUFFIX = ".jar";
    private static final Logger LOGGER = LogUtils.getLogger();
    private final Path modFolder;
    private final String customName;

    public ModsFolderLocator() {
        this(FMLPaths.MODSDIR.get());
    }

    ModsFolderLocator(Path modFolder) {
        this(modFolder, "mods folder");
    }

    public ModsFolderLocator(Path modFolder, String name) {
        this.modFolder = Objects.requireNonNull(modFolder, "modFolder");
        this.customName = Objects.requireNonNull(name, "name");
    }

    @Override
    public void findCandidates(ILaunchContext context, IDiscoveryPipeline pipeline) {
        LOGGER.debug(LogMarkers.SCAN, "Scanning mods dir {} for mods", this.modFolder);

        List<Path> directoryContent;
        try (var files = Files.list(this.modFolder)) {
            directoryContent = files
                    .filter(p -> StringUtils.toLowerCase(p.getFileName().toString()).endsWith(SUFFIX))
                    .sorted(Comparator.comparing(path -> StringUtils.toLowerCase(path.getFileName().toString())))
                    .toList();
        } catch (UncheckedIOException | IOException e) {
            throw new ModLoadingException(ModLoadingIssue.error("fml.modloadingissue.failed_to_list_folder_content", this.modFolder).withAffectedPath(this.modFolder).withCause(e));
        }

        for (var file : directoryContent) {
            if (!Files.isRegularFile(file)) {
                pipeline.addIssue(ModLoadingIssue.warning("fml.modloadingissue.brokenfile.unknown").withAffectedPath(file));
                continue;
            }

            pipeline.addPath(file, ModFileDiscoveryAttributes.DEFAULT, IncompatibleFileReporting.WARN_ALWAYS);
        }
    }

    @Override
    public String toString() {
        return "{" + customName + " locator at " + this.modFolder + "}";
    }
}
