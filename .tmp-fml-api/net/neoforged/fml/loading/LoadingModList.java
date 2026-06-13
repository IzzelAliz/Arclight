/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.loading;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import net.neoforged.fml.ModLoadingIssue;
import net.neoforged.fml.loading.moddiscovery.ModFile;
import net.neoforged.fml.loading.moddiscovery.ModFileInfo;
import net.neoforged.fml.loading.moddiscovery.ModInfo;
import net.neoforged.neoforgespi.language.IModFileInfo;
import net.neoforged.neoforgespi.language.IModInfo;
import net.neoforged.neoforgespi.locating.IModFile;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Master list of all mods <em>in the loading context. This class cannot refer outside the
 * loading package</em>
 */
public class LoadingModList {
    private static final Logger LOG = LoggerFactory.getLogger(LoadingModList.class);

    private final List<IModFileInfo> plugins;
    private final List<IModFile> gameLibraries;
    private final List<ModFileInfo> modFiles;
    private final List<ModInfo> sortedList;
    private final Map<ModInfo, List<ModInfo>> modDependencies;
    private final Map<String, ModFileInfo> fileById;
    @Nullable
    private volatile Map<String, IModFile> fileByPackage;
    private final List<ModLoadingIssue> modLoadingIssues;
    private final Set<IModFile> allModFiles = Collections.newSetFromMap(new IdentityHashMap<>());

    private LoadingModList(List<ModFile> plugins, List<ModFile> gameLibraries, List<ModFile> modFiles, List<ModInfo> sortedList, Map<ModInfo, List<ModInfo>> modDependencies) {
        this.plugins = plugins.stream()
                .map(ModFile::getModFileInfo)
                .collect(Collectors.toList());
        this.gameLibraries = List.copyOf(gameLibraries);
        this.modFiles = modFiles.stream()
                .map(ModFile::getModFileInfo)
                .map(ModFileInfo.class::cast)
                .collect(Collectors.toList());
        this.sortedList = new ArrayList<>(sortedList);
        this.modDependencies = modDependencies;
        this.fileById = this.modFiles.stream()
                .map(ModFileInfo::getMods)
                .flatMap(Collection::stream)
                .map(ModInfo.class::cast)
                .collect(Collectors.toMap(ModInfo::getModId, ModInfo::getOwningFile));
        this.modLoadingIssues = new ArrayList<>();

        this.allModFiles.addAll(this.gameLibraries);
        this.allModFiles.addAll(modFiles);
        this.allModFiles.addAll(plugins);
    }

    public static LoadingModList of(List<ModFile> plugins, List<ModFile> gameLibraries, List<ModFile> modFiles, List<ModInfo> sortedList, List<ModLoadingIssue> issues, Map<ModInfo, List<ModInfo>> modDependencies) {
        var list = new LoadingModList(plugins, gameLibraries, modFiles, sortedList, modDependencies);
        list.modLoadingIssues.addAll(issues);
        return list;
    }

    /**
     * @deprecated Use {@code FMLLoader.getCurrent().getLoadingModList()} instead.
     */
    @Deprecated(forRemoval = true)
    public static LoadingModList get() {
        return FMLLoader.getCurrent().getLoadingModList();
    }

    public boolean contains(IModFile modFile) {
        return allModFiles.contains(modFile);
    }

    public List<IModFileInfo> getPlugins() {
        return plugins;
    }

    public List<IModFile> getGameLibraries() {
        return gameLibraries;
    }

    public List<ModFileInfo> getModFiles() {
        return modFiles;
    }

    /**
     * @return All {@linkplain #getModFiles() mod files}, {@linkplain #getPlugins() plugins} and
     *         {@linkplain #getGameLibraries() game libraries}.
     */
    public Set<IModFile> getAllModFiles() {
        return allModFiles;
    }

    public ModFileInfo getModFileById(String modid) {
        return this.fileById.get(modid);
    }

    public List<ModInfo> getMods() {
        return this.sortedList;
    }

    /**
     * Returns all direct loading dependencies of the given mod.
     *
     * <p>This means: all the mods that are directly specified to be loaded before the given mod,
     * either because the given mod has an {@link IModInfo.Ordering#AFTER} constraint on the dependency,
     * or because the dependency has a {@link IModInfo.Ordering#BEFORE} constraint on the given mod.
     */
    public List<ModInfo> getDependencies(IModInfo mod) {
        return this.modDependencies.getOrDefault(mod, List.of());
    }

    public boolean hasErrors() {
        return !modLoadingIssues.isEmpty() && modLoadingIssues.stream().anyMatch(issue -> issue.severity() == ModLoadingIssue.Severity.ERROR);
    }

    public List<ModLoadingIssue> getModLoadingIssues() {
        return modLoadingIssues;
    }

    Map<String, IModFile> getPackageIndex() {
        var fileByPackage = this.fileByPackage;
        if (fileByPackage == null) {
            synchronized (this) {
                if (this.fileByPackage == null) {
                    this.fileByPackage = buildPackageIndex();
                }
                fileByPackage = this.fileByPackage;
            }
        }
        return fileByPackage;
    }

    private Map<String, IModFile> buildPackageIndex() {
        long start = System.nanoTime();
        Map<String, IModFile> result = new HashMap<>();
        for (var modFile : this.allModFiles) {
            for (var packageName : ((ModFile) modFile).getModuleDescriptor().packages()) {
                result.put(packageName, modFile);
            }
        }
        result = Map.copyOf(result);
        long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
        LOG.debug("Built package index ({} entries) in {}ms", result.size(), elapsed);
        return result;
    }
}
