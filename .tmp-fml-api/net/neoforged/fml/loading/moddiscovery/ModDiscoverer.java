/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.loading.moddiscovery;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.zip.ZipException;
import net.neoforged.fml.ModLoadingException;
import net.neoforged.fml.ModLoadingIssue;
import net.neoforged.fml.i18n.FMLTranslations;
import net.neoforged.fml.jarcontents.JarContents;
import net.neoforged.fml.loading.ImmediateWindowHandler;
import net.neoforged.fml.loading.LogMarkers;
import net.neoforged.fml.loading.UniqueModListBuilder;
import net.neoforged.fml.util.ServiceLoaderUtil;
import net.neoforged.neoforgespi.ILaunchContext;
import net.neoforged.neoforgespi.locating.IDependencyLocator;
import net.neoforged.neoforgespi.locating.IDiscoveryPipeline;
import net.neoforged.neoforgespi.locating.IModFile;
import net.neoforged.neoforgespi.locating.IModFileCandidateLocator;
import net.neoforged.neoforgespi.locating.IModFileReader;
import net.neoforged.neoforgespi.locating.IncompatibleFileReporting;
import net.neoforged.neoforgespi.locating.ModFileDiscoveryAttributes;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

public class ModDiscoverer {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final List<IModFileCandidateLocator> modFileLocators;
    private final List<IDependencyLocator> dependencyLocators;
    private final List<IModFileReader> modFileReaders;
    private final ILaunchContext launchContext;

    public ModDiscoverer(ILaunchContext launchContext) {
        this(launchContext, List.of());
    }

    public ModDiscoverer(ILaunchContext launchContext,
            Collection<IModFileCandidateLocator> additionalModFileLocators) {
        this.launchContext = launchContext;

        modFileLocators = ServiceLoaderUtil.loadEarlyServices(launchContext, IModFileCandidateLocator.class, additionalModFileLocators);
        modFileReaders = ServiceLoaderUtil.loadEarlyServices(launchContext, IModFileReader.class, List.of());
        dependencyLocators = ServiceLoaderUtil.loadEarlyServices(launchContext, IDependencyLocator.class, List.of());
    }

    public record Result(
            List<ModFile> modFiles,
            List<ModLoadingIssue> discoveryIssues) {}

    public Result discoverMods(List<ModFile> additionalDependencySources) {
        LOGGER.debug(LogMarkers.SCAN, "Scanning for mods and other resources to load. We know {} ways to find mods", modFileLocators.size());
        List<ModFile> loadedFiles = new ArrayList<>();
        List<ModLoadingIssue> discoveryIssues = new ArrayList<>();
        boolean successfullyLoadedMods = true;
        ImmediateWindowHandler.updateProgress("Discovering mod files");

        // Loop all mod locators to get the root mods to load from.
        for (var locator : modFileLocators) {
            LOGGER.debug(LogMarkers.SCAN, "Trying locator {}", locator);

            var defaultAttributes = ModFileDiscoveryAttributes.DEFAULT.withLocator(locator);
            var pipeline = new DiscoveryPipeline(defaultAttributes, loadedFiles, discoveryIssues);
            try {
                locator.findCandidates(launchContext, pipeline);
            } catch (ModLoadingException e) {
                discoveryIssues.addAll(e.getIssues());
            } catch (Exception e) {
                discoveryIssues.add(ModLoadingIssue.error("fml.modloadingissue.technical_error", locator.toString() + " failed").withCause(e));
            }

            LOGGER.debug(LogMarkers.SCAN, "Locator {} found {} mods, {} warnings, {} errors and skipped {} candidates", locator,
                    pipeline.successCount, pipeline.warningCount, pipeline.errorCount, pipeline.skipCount);
        }

        //First processing run of the mod list. Any duplicates will cause resolution failure and dependency loading will be skipped.
        Map<IModFile.Type, List<ModFile>> modFilesMap = Collections.emptyMap();
        try {
            UniqueModListBuilder modsUniqueListBuilder = new UniqueModListBuilder(loadedFiles);
            UniqueModListBuilder.UniqueModListData uniqueModsData = modsUniqueListBuilder.buildUniqueList();
            uniqueModsData.discardedFiles().forEach(ModFile::close);

            //Grab the temporary results.
            //This allows loading to continue to a base state, in case dependency loading fails.
            modFilesMap = uniqueModsData.modFiles().stream()
                    .collect(Collectors.groupingBy(IModFile::getType));
            loadedFiles = uniqueModsData.modFiles();
        } catch (ModLoadingException exception) {
            LOGGER.error(LogMarkers.SCAN, "Failed to build unique mod list after mod discovery.", exception);
            discoveryIssues.addAll(exception.getIssues());
            successfullyLoadedMods = false;
            loadedFiles.forEach(ModFile::close);
        }

        // We can continue loading if prime mods loaded successfully.
        if (successfullyLoadedMods) {
            LOGGER.debug(LogMarkers.SCAN, "Successfully Loaded {} mods. Attempting to load dependencies...", loadedFiles.size());

            List<IModFile> dependencySources = new ArrayList<>(loadedFiles);
            dependencySources.addAll(additionalDependencySources);
            dependencySources = List.copyOf(dependencySources);

            for (var locator : dependencyLocators) {
                try {
                    LOGGER.debug(LogMarkers.SCAN, "Trying locator {}", locator);
                    var pipeline = new DiscoveryPipeline(ModFileDiscoveryAttributes.DEFAULT.withDependencyLocator(locator), loadedFiles, discoveryIssues);
                    locator.scanMods(dependencySources, pipeline);
                } catch (ModLoadingException exception) {
                    LOGGER.error(LogMarkers.SCAN, "Failed to load dependencies with locator {}", locator, exception);
                    discoveryIssues.addAll(exception.getIssues());
                }
            }

            //Second processing run of the mod list. Any duplicates will cause resolution failure and only the mods list will be loaded.
            try {
                UniqueModListBuilder modsAndDependenciesUniqueListBuilder = new UniqueModListBuilder(loadedFiles);
                UniqueModListBuilder.UniqueModListData uniqueModsAndDependenciesData = modsAndDependenciesUniqueListBuilder.buildUniqueList();
                uniqueModsAndDependenciesData.discardedFiles().forEach(ModFile::close);

                //We now only need the mod files map, not the list.
                modFilesMap = uniqueModsAndDependenciesData.modFiles().stream()
                        .collect(Collectors.groupingBy(IModFile::getType));
            } catch (ModLoadingException exception) {
                LOGGER.error(LogMarkers.SCAN, "Failed to build unique mod list after dependency discovery.", exception);
                discoveryIssues.addAll(exception.getIssues());
                modFilesMap = loadedFiles.stream().collect(Collectors.groupingBy(IModFile::getType));
            }
        } else {
            //Failure notify the listeners.
            LOGGER.error(LogMarkers.SCAN, "Mod Discovery failed. Skipping dependency discovery.");
        }

        LOGGER.info("\n     Mod List:\n\t\tName Version (Mod Id)\n\n{}", logReport(modFilesMap.values()));

        var modFiles = new ArrayList<ModFile>();
        modFilesMap.values().forEach(modFiles::addAll);
        return new Result(modFiles, discoveryIssues);
    }

    private String logReport(Collection<List<ModFile>> modFiles) {
        return modFiles.stream()
                .flatMap(Collection::stream)
                .filter(modFile -> !modFile.getModInfos().isEmpty())
                .sorted(Comparator.comparing(modFile -> modFile.getModInfos().getFirst().getDisplayName(), String.CASE_INSENSITIVE_ORDER))
                .map(this::fileToLine)
                .collect(Collectors.joining("\n\t\t", "\t\t", ""));
    }

    private String fileToLine(IModFile mf) {
        var mainMod = mf.getModInfos().getFirst();

        return String.format(Locale.ENGLISH, "%s %s (%s)",
                mainMod.getDisplayName(),
                mainMod.getVersion(),
                mainMod.getModId());
    }

    private class DiscoveryPipeline implements IDiscoveryPipeline {
        private final ModFileDiscoveryAttributes defaultAttributes;
        private final List<ModFile> loadedFiles;
        private final List<ModLoadingIssue> issues;

        private int successCount;
        private int errorCount;
        private int warningCount;
        private int skipCount;

        public DiscoveryPipeline(ModFileDiscoveryAttributes defaultAttributes,
                List<ModFile> loadedFiles,
                List<ModLoadingIssue> issues) {
            this.defaultAttributes = defaultAttributes;
            this.loadedFiles = loadedFiles;
            this.issues = issues;
        }

        @Override
        public Optional<IModFile> addPath(List<Path> groupedPaths, ModFileDiscoveryAttributes attributes, IncompatibleFileReporting reporting) {
            var primaryPath = groupedPaths.getFirst();

            if (!launchContext.addLocated(primaryPath)) {
                LOGGER.debug("Skipping {} because it was already located earlier", primaryPath);
                skipCount++;
                return Optional.empty();
            }

            JarContents jarContents;
            try {
                jarContents = JarContents.ofPaths(groupedPaths);
            } catch (Exception e) {
                if (causeChainContains(e, ZipException.class)) {
                    addIssue(ModLoadingIssue.error("fml.modloadingissue.brokenfile.invalidzip").withAffectedPath(primaryPath).withCause(e));
                } else {
                    addIssue(ModLoadingIssue.error("fml.modloadingissue.brokenfile").withAffectedPath(primaryPath).withCause(e));
                }
                return Optional.empty();
            }

            return addJarContent(jarContents, attributes, reporting);
        }

        @Override
        public @Nullable IModFile readModFile(JarContents jarContents, ModFileDiscoveryAttributes attributes) {
            for (var reader : modFileReaders) {
                var provided = reader.read(jarContents, attributes);
                if (provided != null) {
                    return provided;
                }
            }

            throw new RuntimeException("No mod reader felt responsible for " + jarContents.getPrimaryPath());
        }

        @Override
        public Optional<IModFile> addJarContent(JarContents jarContents, ModFileDiscoveryAttributes attributes, IncompatibleFileReporting reporting) {
            attributes = defaultAttributes.merge(attributes);

            List<ModLoadingIssue> incompatibilityIssues = new ArrayList<>();

            for (var reader : modFileReaders) {
                try {
                    var provided = reader.read(jarContents, attributes);
                    if (provided != null) {
                        if (addModFile(provided)) {
                            return Optional.of(provided);
                        }
                        // The reader might have returned something other than a ModFile (that is one reason for addModFile rejecting it)
                        if (provided instanceof ModFile modFile) {
                            modFile.close();
                        } else {
                            closeJarContents(jarContents);
                        }
                        return Optional.empty();
                    }
                } catch (ModLoadingException e) {
                    // The reader didn't outright crash but reported reasons for incompatibility.
                    // We'll stash them here in case no other reader successfully reads the file.
                    incompatibilityIssues.addAll(e.getIssues());
                } catch (Exception e) {
                    closeJarContents(jarContents); // Ensure the jar contents are closed

                    // When a reader just outright crashes while reading the file, we do error intentionally.
                    addIssue(ModLoadingIssue.error("fml.modloadingissue.brokenfile").withAffectedPath(jarContents.getPrimaryPath()).withCause(e));
                    return Optional.empty();
                }
            }

            // If a jar file was found in a subdirectory of the game directory, but could not be loaded,
            // it might be an incompatible mod type. We do not perform this validation for jars that we
            // found on the classpath or other locations since these are usually not under user control.
            if (reporting != IncompatibleFileReporting.IGNORE) {
                // Detect additional, potential reasons for the jar being incompatible
                var reason = IncompatibleModReason.detect(jarContents);
                if (reason.isPresent()) {
                    incompatibilityIssues.add(ModLoadingIssue.error(reason.get().getReason()).withAffectedPath(jarContents.getPrimaryPath()));
                } else if (reporting != IncompatibleFileReporting.WARN_ON_KNOWN_INCOMPATIBILITY && incompatibilityIssues.isEmpty()) {
                    incompatibilityIssues.add(ModLoadingIssue.error("fml.modloadingissue.brokenfile.unknown").withAffectedPath(jarContents.getPrimaryPath()));
                }

                for (var issue : incompatibilityIssues) {
                    // Convert the issue to the desired reporting severity
                    issue = issue.withSeverity(reporting.getIssueSeverity());

                    LOGGER.atLevel(reporting.getLogLevel())
                            .addMarker(LogMarkers.SCAN)
                            .log("Skipping jar. {}", FMLTranslations.translateIssueEnglish(issue));
                    addIssue(issue);
                }
            }

            // No reader successfully parsed the contents, ensure they're properly closed now
            closeJarContents(jarContents);

            return Optional.empty();
        }

        @Override
        public boolean addModFile(IModFile mf) {
            Objects.requireNonNull(mf, "mf");
            if (!(mf instanceof ModFile modFile)) {
                String detail = "Unexpected IModFile subclass: " + mf.getClass();
                addIssue(ModLoadingIssue.error("fml.modloadingissue.technical_error", detail).withAffectedModFile(mf));
                return false;
            }

            modFile.setDiscoveryAttributes(defaultAttributes.merge(mf.getDiscoveryAttributes()));

            var discoveryAttributes = mf.getDiscoveryAttributes();
            LOGGER.info(LogMarkers.SCAN, "Found {} file \"{}\" {}", mf.getType().name().toLowerCase(Locale.ROOT), mf.getFileName(), discoveryAttributes);

            loadedFiles.add(modFile);
            successCount++;
            return true;
        }

        @Override
        public void addIssue(ModLoadingIssue issue) {
            issues.add(issue);
            switch (issue.severity()) {
                case WARNING -> warningCount++;
                case ERROR -> errorCount++;
            }
        }
    }

    private static void closeJarContents(JarContents jarContents) {
        try {
            jarContents.close();
        } catch (IOException e) {
            LOGGER.error("Failed to close jar contents {}", jarContents, e);
        }
    }

    private static boolean causeChainContains(Throwable e, Class<?> exceptionClass) {
        for (; e != null; e = e.getCause()) {
            if (exceptionClass.isInstance(e)) {
                return true;
            }
        }
        return false;
    }
}
