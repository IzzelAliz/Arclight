/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.neoforged.fml.loading.moddiscovery.ModFile;
import net.neoforged.fml.loading.moddiscovery.ModFileInfo;
import net.neoforged.fml.loading.moddiscovery.ModInfo;
import net.neoforged.neoforgespi.language.IModFileInfo;
import net.neoforged.neoforgespi.language.IModInfo;
import net.neoforged.neoforgespi.language.ModFileScanData;
import net.neoforged.neoforgespi.locating.IModFile;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.VisibleForTesting;

/**
 * Master list of all mods.
 */
public class ModList {
    private static ModList INSTANCE;
    private final List<IModFileInfo> modFiles;
    private final List<IModInfo> sortedList;
    private final Map<String, ModFileInfo> fileById;
    private List<ModContainer> mods;
    private Map<String, ModContainer> indexedMods;
    private List<ModFileScanData> modFileScanData;
    private List<ModContainer> sortedContainers;

    private ModList(List<ModFile> modFiles, List<ModInfo> sortedList) {
        this.modFiles = modFiles.stream().map(ModFile::getModFileInfo).toList();
        this.sortedList = sortedList.stream().map(IModInfo.class::cast).toList();
        this.fileById = this.modFiles.stream().map(IModFileInfo::getMods).flatMap(Collection::stream).map(ModInfo.class::cast).collect(Collectors.toUnmodifiableMap(ModInfo::getModId, ModInfo::getOwningFile));
        CrashReportCallables.registerCrashCallable("Mod List", this::crashReport);
    }

    private String fileToLine(IModFile mf) {
        var mainMod = mf.getModInfos().getFirst();
        return String.format(Locale.ENGLISH, "%-50.50s|%-30.30s|%-30.30s|%-20.20s|Manifest: %s", mf.getFileName(),
                mainMod.getDisplayName(),
                mainMod.getModId(),
                mainMod.getVersion(),
                ((ModFileInfo) mf.getModFileInfo()).getCodeSigningFingerprint().orElse("NOSIGNATURE"));
    }

    private String crashReport() {
        return "\n" + applyForEachModFileAlphabetical(this::fileToLine).collect(Collectors.joining("\n\t\t", "\t\t", ""));
    }

    public static ModList of(List<ModFile> modFiles, List<ModInfo> sortedList) {
        INSTANCE = new ModList(modFiles, sortedList);
        return INSTANCE;
    }

    public static ModList get() {
        return INSTANCE;
    }

    public List<IModFileInfo> getModFiles() {
        return modFiles;
    }

    public IModFileInfo getModFileById(String modid) {
        return this.fileById.get(modid);
    }

    static CompletionStage<Void> completableFutureFromExceptionList(List<? extends Map.Entry<?, Throwable>> t) {
        if (t.stream().noneMatch(e -> e.getValue() != null)) {
            return CompletableFuture.completedFuture(null);
        } else {
            List<Throwable> throwables = t.stream().filter(e -> e.getValue() != null).map(Map.Entry::getValue).collect(Collectors.toList());
            CompletableFuture<Void> cf = new CompletableFuture<>();
            RuntimeException accumulator = new RuntimeException();
            cf.completeExceptionally(accumulator);
            throwables.forEach(exception -> {
                if (exception instanceof CompletionException) {
                    exception = exception.getCause();
                }
                if (exception.getSuppressed().length != 0) {
                    Arrays.stream(exception.getSuppressed()).forEach(accumulator::addSuppressed);
                } else {
                    accumulator.addSuppressed(exception);
                }
            });
            return cf;
        }
    }

    static <V> CompletableFuture<List<Map.Entry<V, Throwable>>> gather(List<? extends CompletableFuture<? extends V>> futures) {
        List<Map.Entry<V, Throwable>> list = new ArrayList<>(futures.size());
        CompletableFuture<?>[] results = new CompletableFuture[futures.size()];
        futures.forEach(future -> {
            int i = list.size();
            list.add(null);
            results[i] = future.whenComplete((result, exception) -> list.set(i, new AbstractMap.SimpleImmutableEntry<>(result, exception)));
        });
        return CompletableFuture.allOf(results).handle((r, th) -> null).thenApply(res -> list);
    }

    void setLoadedMods(List<ModContainer> modContainers) {
        this.mods = modContainers;
        this.sortedContainers = modContainers.stream().sorted(Comparator.comparingInt(c -> sortedList.indexOf(c.getModInfo()))).toList();
        this.indexedMods = modContainers.stream().collect(Collectors.toMap(ModContainer::getModId, Function.identity()));
    }

    public Optional<? extends ModContainer> getModContainerById(String modId) {
        return Optional.ofNullable(this.indexedMods.get(modId));
    }

    public List<IModInfo> getMods() {
        return this.sortedList;
    }

    public boolean isLoaded(String modTarget) {
        return this.indexedMods.containsKey(modTarget);
    }

    public int size() {
        return mods.size();
    }

    public List<ModFileScanData> getAllScanData() {
        if (modFileScanData == null) {
            modFileScanData = this.sortedList.stream().map(IModInfo::getOwningFile).filter(Objects::nonNull).map(IModFileInfo::getFile).distinct().map(IModFile::getScanResult).collect(Collectors.toList());
        }
        return modFileScanData;
    }

    public void forEachModFile(Consumer<IModFile> fileConsumer) {
        modFiles.stream().map(IModFileInfo::getFile).forEach(fileConsumer);
    }

    public <T> Stream<T> applyForEachModFile(Function<IModFile, T> function) {
        return modFiles.stream().map(IModFileInfo::getFile).map(function);
    }

    /**
     * Stream sorted by Mod Name in alphabetical order
     */
    public <T> Stream<T> applyForEachModFileAlphabetical(Function<IModFile, T> function) {
        return modFiles.stream()
                .map(IModFileInfo::getFile)
                .sorted(Comparator.comparing(modFile -> modFile.getModInfos().getFirst().getDisplayName(), String.CASE_INSENSITIVE_ORDER))
                .map(function);
    }

    public void forEachModContainer(BiConsumer<String, ModContainer> modContainerConsumer) {
        indexedMods.forEach(modContainerConsumer);
    }

    public List<ModContainer> getSortedMods() {
        return sortedContainers;
    }

    public void forEachModInOrder(Consumer<ModContainer> containerConsumer) {
        this.sortedContainers.forEach(containerConsumer);
    }

    public <T> Stream<T> applyForEachModContainer(Function<ModContainer, T> function) {
        return indexedMods.values().stream().map(function);
    }

    @ApiStatus.Internal
    @VisibleForTesting
    public static void clear() {
        INSTANCE = null;
    }
}
