/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.loading;

import static java.util.stream.Collectors.flatMapping;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.neoforged.fml.ModLoadingException;
import net.neoforged.fml.ModLoadingIssue;
import net.neoforged.fml.loading.moddiscovery.ModFile;
import net.neoforged.fml.loading.moddiscovery.ModFileInfo;
import net.neoforged.fml.loading.moddiscovery.ModInfo;
import net.neoforged.fml.loading.toposort.CyclePresentException;
import net.neoforged.fml.loading.toposort.TopologicalSort;
import net.neoforged.neoforgespi.language.IModInfo;
import net.neoforged.neoforgespi.locating.IModFile;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.slf4j.Logger;

class ModSorter {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final UniqueModListBuilder uniqueModListBuilder;
    private List<ModFile> modFiles;
    private List<ModInfo> sortedList;
    private Map<ModInfo, List<ModInfo>> modDependencies;
    private Map<String, IModInfo> modIdNameLookup;
    private List<ModFile> systemMods;

    private ModSorter(List<ModFile> modFiles) {
        this.uniqueModListBuilder = new UniqueModListBuilder(modFiles);
    }

    public static LoadingModList sort(List<ModFile> modFiles, List<ModLoadingIssue> issues) {
        var gameContent = new ArrayList<ModFile>(modFiles.size());
        var gameLibraryContent = new ArrayList<ModFile>(modFiles.size());
        var pluginContent = new ArrayList<ModFile>(modFiles.size());
        for (var modFile : modFiles) {
            if (modFile.getType() == IModFile.Type.LIBRARY) {
                pluginContent.add(modFile);
            } else if (modFile.getType() == IModFile.Type.GAMELIBRARY) {
                gameLibraryContent.add(modFile);
            } else {
                gameContent.add(modFile);
            }
        }
        return sort(pluginContent, gameLibraryContent, gameContent, issues);
    }

    public static LoadingModList sort(List<ModFile> plugins, List<ModFile> gameLibraries, List<ModFile> mods, List<ModLoadingIssue> issues) {
        ModSorter ms = new ModSorter(mods);
        try {
            ms.buildUniqueList();
        } catch (ModLoadingException e) {
            // We cannot build any list with duped mods. We have to abort immediately and report it
            return LoadingModList.of(plugins, gameLibraries, ms.systemMods, ms.systemMods.stream().map(mf -> (ModInfo) mf.getModInfos().get(0)).collect(toList()), concat(issues, e.getIssues()), Map.of());
        }

        // try and validate dependencies
        DependencyResolutionResult resolutionResult = ms.verifyDependencyVersions();

        LoadingModList list;

        // if we miss a dependency or detect an incompatibility, we abort now
        if (!resolutionResult.versionResolution.isEmpty() || !resolutionResult.incompatibilities.isEmpty()) {
            list = LoadingModList.of(plugins, gameLibraries, ms.systemMods, ms.systemMods.stream().map(mf -> (ModInfo) mf.getModInfos().get(0)).collect(toList()), concat(issues, resolutionResult.buildErrorMessages()), Map.of());
        } else {
            // Otherwise, lets try and sort the modlist and proceed
            ModLoadingException modLoadingException = null;
            try {
                ms.sort(issues);
            } catch (ModLoadingException e) {
                modLoadingException = e;
            }
            if (modLoadingException == null) {
                list = LoadingModList.of(plugins, gameLibraries, ms.modFiles, ms.sortedList, issues, ms.modDependencies);
            } else {
                list = LoadingModList.of(plugins, gameLibraries, ms.modFiles, ms.sortedList, concat(issues, modLoadingException.getIssues()), Map.of());
            }
        }

        // If we have conflicts those are considered warnings
        if (!resolutionResult.discouraged.isEmpty()) {
            list.getModLoadingIssues().add(ModLoadingIssue.warning(
                    "found mod conflicts",
                    resolutionResult.buildWarningMessages()));
        }

        // Close any mod-files discarded due to dependency constraint issues
        Set<ModFile> loadedMods = Collections.newSetFromMap(new IdentityHashMap<>());
        list.getModFiles().forEach(mfi -> loadedMods.add(mfi.getFile()));
        for (var modFile : ms.modFiles) {
            if (!loadedMods.contains(modFile)) {
                modFile.close();
            }
        }

        return list;
    }

    @SafeVarargs
    private static <T> List<T> concat(List<T>... lists) {
        var lst = new ArrayList<T>();
        for (List<T> list : lists) {
            lst.addAll(list);
        }
        return lst;
    }

    @SuppressWarnings("UnstableApiUsage")
    private void sort(List<ModLoadingIssue> issues) {
        // lambdas are identity based, so sorting them is impossible unless you hold reference to them
        MutableGraph<ModInfo> graph = GraphBuilder.directed().build();
        AtomicInteger counter = new AtomicInteger();
        Map<ModInfo, Integer> infos = modFiles.stream()
                .flatMap(mf -> mf.getModInfos().stream())
                .map(ModInfo.class::cast)
                .collect(toMap(Function.identity(), e -> counter.incrementAndGet()));
        infos.keySet().forEach(graph::addNode);
        modFiles.stream()
                .map(ModFile::getModInfos)
                .<IModInfo>mapMulti(Iterable::forEach)
                .map(IModInfo::getDependencies).<IModInfo.ModVersion>mapMulti(Iterable::forEach)
                .forEach(dep -> addDependency(graph, dep));

        // now consider dependency overrides
        // we also check their validity here, and report unknown mods as warnings
        FMLConfig.getDependencyOverrides().forEach((id, overrides) -> {
            var target = (ModInfo) modIdNameLookup.get(id);
            if (target == null) {
                issues.add(ModLoadingIssue.warning("fml.modloadingissue.depoverride.unknown_target", id));
            } else {
                for (FMLConfig.DependencyOverride override : overrides) {
                    var dep = (ModInfo) modIdNameLookup.get(override.modId());
                    if (dep == null) {
                        issues.add(ModLoadingIssue.warning("fml.modloadingissue.depoverride.unknown_dependency", override.modId(), id));
                    } else if (!override.remove()) {
                        // Add ordering dependency overrides (random order -> target AFTER dependency)
                        // We do not need to check for overrides that attempt to change the declared order as the sorter will detect the cycle itself and error
                        graph.putEdge(dep, target);
                    }
                }
            }
        });

        List<ModInfo> sorted;
        try {
            sorted = TopologicalSort.topologicalSort(graph, Comparator.comparing(infos::get));
        } catch (CyclePresentException e) {
            Set<Set<ModFileInfo>> cycles = e.getCycles();
            if (LOGGER.isErrorEnabled(LogMarkers.LOADING)) {
                LOGGER.error(LogMarkers.LOADING, "Mod Sorting failed.\nDetected Cycles: {}\n", cycles);
            }
            var dataList = cycles.stream()
                    .<ModFileInfo>mapMulti(Iterable::forEach)
                    .<IModInfo>mapMulti((mf, c) -> mf.getMods().forEach(c))
                    .map(IModInfo::getModId)
                    .map(list -> ModLoadingIssue.error("fml.modloadingissue.cycle", list).withCause(e))
                    .toList();
            throw new ModLoadingException(dataList);
        }
        this.sortedList = List.copyOf(sorted);
        this.modDependencies = sorted.stream()
                .collect(Collectors.toMap(modInfo -> modInfo, modInfo -> List.copyOf(graph.predecessors(modInfo))));
        this.modFiles = sorted.stream()
                .map(mi -> mi.getOwningFile().getFile())
                .distinct()
                .toList();
    }

    @SuppressWarnings("UnstableApiUsage")
    private void addDependency(MutableGraph<ModInfo> topoGraph, IModInfo.ModVersion dep) {
        ModInfo self = (ModInfo) dep.getOwner();
        IModInfo targetModInfo = modIdNameLookup.get(dep.getModId());
        // soft dep that doesn't exist. Just return. No edge required.
        if (!(targetModInfo instanceof ModInfo target)) return;
        if (self == target)
            return; // in case a jar has two mods that have dependencies between
        switch (dep.getOrdering()) {
            case BEFORE -> topoGraph.putEdge(self, target);
            case AFTER -> topoGraph.putEdge(target, self);
        }
    }

    private void buildUniqueList() {
        UniqueModListBuilder.UniqueModListData uniqueModListData = uniqueModListBuilder.buildUniqueList();
        uniqueModListData.discardedFiles().forEach(ModFile::close);

        this.modFiles = uniqueModListData.modFiles();

        detectSystemMods(uniqueModListData.modFilesByFirstId());

        modIdNameLookup = uniqueModListData.modFiles().stream()
                .flatMap(mf -> mf.getModInfos().stream())
                .collect(Collectors.toMap(IModInfo::getModId, mi -> mi));
    }

    private void detectSystemMods(Map<String, List<ModFile>> modFilesByFirstId) {
        // Capture system mods (ex. MC, Forge) here, so we can keep them for later
        Set<String> systemMods = new HashSet<>();
        // The minecraft and neoforge mods are always system mods
        systemMods.add("minecraft");
        systemMods.add("neoforge");
        // Find system mod files and scan them for system mods
        modFiles.stream()
                .map(mf -> mf.getContents().getManifest().getMainAttributes().getValue("FML-System-Mods"))
                .filter(Objects::nonNull)
                .findFirst()
                .ifPresent(value -> systemMods.addAll(Arrays.asList(value.split(","))));
        LOGGER.debug("Configured system mods: {}", systemMods);

        this.systemMods = new ArrayList<>();
        for (String systemMod : systemMods) {
            var container = modFilesByFirstId.get(systemMod);
            if (container != null && !container.isEmpty()) {
                LOGGER.debug("Found system mod: {}", systemMod);
                this.systemMods.add(container.getFirst());
            }
        }
    }

    public record DependencyResolutionResult(
            Collection<IModInfo.ModVersion> incompatibilities,
            Collection<IModInfo.ModVersion> discouraged,
            Collection<IModInfo.ModVersion> versionResolution,
            Map<String, ArtifactVersion> modVersions) {
        public List<ModLoadingIssue> buildWarningMessages() {
            return Stream.concat(discouraged.stream()
                    .map(mv -> ModLoadingIssue.warning("fml.modloadingissue.discouragedmod",
                            mv.getModId(), mv.getOwner().getModId(), mv.getVersionRange(),
                            modVersions.get(mv.getModId()), mv.getReason().orElse("fml.modloadingissue.discouragedmod.noreason")).withAffectedMod(mv.getOwner())),

                    Stream.of(ModLoadingIssue.warning("fml.modloadingissue.discouragedmod.proceed")))
                    .toList();
        }

        public List<ModLoadingIssue> buildErrorMessages() {
            return Stream.concat(
                    versionResolution.stream()
                            .map(mv -> ModLoadingIssue.error(mv.getType() == IModInfo.DependencyType.REQUIRED ? "fml.modloadingissue.missingdependency" : "fml.modloadingissue.missingdependency.optional",
                                    mv.getModId(), mv.getOwner().getModId(), mv.getVersionRange(),
                                    modVersions.getOrDefault(mv.getModId(), new DefaultArtifactVersion("null")), mv.getReason()).withAffectedMod(mv.getOwner())),
                    incompatibilities.stream()
                            .map(mv -> ModLoadingIssue.error("fml.modloadingissue.incompatiblemod",
                                    mv.getModId(), mv.getOwner().getModId(), mv.getVersionRange(),
                                    modVersions.get(mv.getModId()), mv.getReason().orElse("fml.modloadingissue.incompatiblemod.noreason")).withAffectedMod(mv.getOwner())))
                    .toList();
        }
    }

    private DependencyResolutionResult verifyDependencyVersions() {
        var modVersions = modFiles.stream()
                .map(ModFile::getModInfos)
                .<IModInfo>mapMulti(Iterable::forEach)
                .collect(toMap(IModInfo::getModId, IModInfo::getVersion));

        var modVersionDependencies = modFiles.stream()
                .map(ModFile::getModInfos)
                .<IModInfo>mapMulti(Iterable::forEach)
                .collect(groupingBy(Function.identity(), flatMapping(e -> {
                    var overrides = FMLConfig.getOverrides(e.getModId());
                    // consider overrides and invalidate dependencies that are removed
                    if (!overrides.isEmpty()) {
                        var ids = overrides.stream()
                                .filter(FMLConfig.DependencyOverride::remove)
                                .map(FMLConfig.DependencyOverride::modId)
                                .collect(toSet());
                        return e.getDependencies().stream()
                                .filter(v -> !ids.contains(v.getModId()));
                    }
                    return e.getDependencies().stream();
                }, toList())));

        var modRequirements = modVersionDependencies.values().stream().<IModInfo.ModVersion>mapMulti(Iterable::forEach)
                .filter(mv -> mv.getSide().isCorrectSide())
                .collect(toSet());

        long mandatoryRequired = modRequirements.stream().filter(ver -> ver.getType() == IModInfo.DependencyType.REQUIRED).count();
        LOGGER.debug(LogMarkers.LOADING, "Found {} mod requirements ({} mandatory, {} optional)", modRequirements.size(), mandatoryRequired, modRequirements.size() - mandatoryRequired);
        var missingVersions = modRequirements.stream()
                .filter(mv -> (mv.getType() == IModInfo.DependencyType.REQUIRED || (modVersions.containsKey(mv.getModId()) && mv.getType() == IModInfo.DependencyType.OPTIONAL)) && this.modVersionNotContained(mv, modVersions))
                .collect(toSet());
        long mandatoryMissing = missingVersions.stream().filter(mv -> mv.getType() == IModInfo.DependencyType.REQUIRED).count();
        LOGGER.debug(LogMarkers.LOADING, "Found {} mod requirements missing ({} mandatory, {} optional)", missingVersions.size(), mandatoryMissing, missingVersions.size() - mandatoryMissing);

        var incompatibleVersions = modRequirements.stream().filter(ver -> ver.getType() == IModInfo.DependencyType.INCOMPATIBLE)
                .filter(ver -> modVersions.containsKey(ver.getModId()) && !this.modVersionNotContained(ver, modVersions))
                .collect(toSet());

        var discouragedVersions = modRequirements.stream().filter(ver -> ver.getType() == IModInfo.DependencyType.DISCOURAGED)
                .filter(ver -> modVersions.containsKey(ver.getModId()) && !this.modVersionNotContained(ver, modVersions))
                .collect(toSet());

        if (!discouragedVersions.isEmpty()) {
            LOGGER.error(
                    LogMarkers.LOADING,
                    "Conflicts between mods:\n{}\n\tIssues may arise. Continue at your own risk.",
                    discouragedVersions.stream()
                            .map(ver -> formatIncompatibleDependencyError(ver, "discourages", modVersions))
                            .collect(Collectors.joining("\n")));
        }

        if (mandatoryMissing > 0) {
            LOGGER.error(
                    LogMarkers.LOADING,
                    "Missing or unsupported mandatory dependencies:\n{}",
                    missingVersions.stream()
                            .filter(mv -> mv.getType() == IModInfo.DependencyType.REQUIRED)
                            .map(ver -> formatDependencyError(ver, modVersions))
                            .collect(Collectors.joining("\n")));
        }
        if (missingVersions.size() - mandatoryMissing > 0) {
            LOGGER.error(
                    LogMarkers.LOADING,
                    "Unsupported installed optional dependencies:\n{}",
                    missingVersions.stream()
                            .filter(ver -> ver.getType() == IModInfo.DependencyType.OPTIONAL)
                            .map(ver -> formatDependencyError(ver, modVersions))
                            .collect(Collectors.joining("\n")));
        }

        if (!incompatibleVersions.isEmpty()) {
            LOGGER.error(
                    LogMarkers.LOADING,
                    "Incompatibilities between mods:\n{}",
                    incompatibleVersions.stream()
                            .map(ver -> formatIncompatibleDependencyError(ver, "is incompatible with", modVersions))
                            .collect(Collectors.joining("\n")));
        }

        return new DependencyResolutionResult(incompatibleVersions, discouragedVersions, missingVersions, modVersions);
    }

    private static String formatDependencyError(IModInfo.ModVersion dependency, Map<String, ArtifactVersion> modVersions) {
        ArtifactVersion installed = modVersions.get(dependency.getModId());
        return String.format(
                "\tMod ID: '%s', Requested by: '%s', Expected range: '%s', Actual version: '%s'",
                dependency.getModId(),
                dependency.getOwner().getModId(),
                dependency.getVersionRange(),
                installed != null ? installed.toString() : "[MISSING]");
    }

    private static String formatIncompatibleDependencyError(IModInfo.ModVersion dependency, String type, Map<String, ArtifactVersion> modVersions) {
        return String.format(
                "\tMod '%s' %s '%s', versions: '%s'; Version found: '%s'",
                dependency.getOwner().getModId(),
                type,
                dependency.getModId(),
                dependency.getVersionRange(),
                modVersions.get(dependency.getModId()).toString());
    }

    private boolean modVersionNotContained(IModInfo.ModVersion mv, Map<String, ArtifactVersion> modVersions) {
        var versionSupportMatrix = FMLLoader.getCurrent().getVersionSupportMatrix();

        return !(versionSupportMatrix.testVersionSupportMatrix(mv.getVersionRange(), mv.getModId(), "mod", (modId, range) -> {
            return modVersions.containsKey(modId) &&
                    (range.containsVersion(modVersions.get(modId)) || modVersions.get(modId).toString().equals("0.0NONE"));
        }));
    }
}
