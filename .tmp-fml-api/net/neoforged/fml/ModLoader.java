/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml;

import static net.neoforged.fml.Logging.CORE;
import static net.neoforged.fml.Logging.LOADING;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.event.IModBusEvent;
import net.neoforged.fml.event.lifecycle.FMLConstructModEvent;
import net.neoforged.fml.event.lifecycle.ParallelDispatchEvent;
import net.neoforged.fml.i18n.FMLTranslations;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.LoadingModList;
import net.neoforged.fml.loading.moddiscovery.ModFileInfo;
import net.neoforged.fml.loading.progress.StartupNotificationManager;
import net.neoforged.neoforgespi.language.IModInfo;
import net.neoforged.neoforgespi.language.IModLanguageLoader;
import net.neoforged.neoforgespi.language.ModFileScanData;
import net.neoforged.neoforgespi.locating.ForgeFeature;
import net.neoforged.neoforgespi.locating.IModFile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

/**
 * Contains the logic to load mods, i.e. turn the {@link LoadingModList} into the {@link ModList},
 * as well as initialization tasks for mods and methods to dispatch mod bus events.
 *
 * <p>For the mod initialization flow, see {@code CommonModLoader} in NeoForge.
 *
 * <p>For mod bus event dispatch, see {@link #postEvent(Event)} and related methods.
 */
public final class ModLoader {
    private ModLoader() {}

    private static final Logger LOGGER = LogManager.getLogger();

    private static final List<ModLoadingIssue> loadingIssues = new ArrayList<>();
    private static ModList modList;

    /**
     * Run on the primary starting thread by ClientModLoader and ServerModLoader
     *
     * @param syncExecutor     An executor to run tasks on the main thread
     * @param parallelExecutor An executor to run tasks on a parallel loading thread pool
     * @param periodicTask     Optional periodic task to perform on the main thread while other activities run
     */
    public static void gatherAndInitializeMods(Executor syncExecutor, Executor parallelExecutor, Runnable periodicTask) {
        var loadingModList = FMLLoader.getCurrent().getLoadingModList();
        loadingIssues.addAll(loadingModList.getModLoadingIssues());
        throwIfErrors(null);

        ForgeFeature.registerFeature("javaVersion", ForgeFeature.VersionFeatureTest.forVersionString(IModInfo.DependencySide.BOTH, System.getProperty("java.version")));
        FMLLoader.getCurrent().backgroundScanHandler.waitForScanToComplete(periodicTask);
        ModList modList = ModList.of(loadingModList.getModFiles().stream().map(ModFileInfo::getFile).toList(),
                loadingModList.getMods());
        throwIfErrors(modList);

        for (var mod : loadingModList.getMods()) {
            for (var bound : mod.getForgeFeatures()) {
                if (!ForgeFeature.testFeature(FMLLoader.getCurrent().getDist(), bound)) {
                    loadingIssues.add(ModLoadingIssue.error("fml.modloadingissue.feature.missing", bound, ForgeFeature.featureValue(bound)).withAffectedMod(mod));
                }
            }
        }
        throwIfErrors(modList);

        var modContainers = loadingModList.getModFiles().stream()
                .map(ModFileInfo::getFile)
                .map(ModLoader::buildMods)
                .<ModContainer>mapMulti(Iterable::forEach)
                .toList();
        throwIfErrors(modList);

        modList.setLoadedMods(modContainers);
        ModLoader.modList = modList;

        constructMods(syncExecutor, parallelExecutor, periodicTask);
    }

    private static void throwIfErrors(@Nullable ModList modList) {
        if (hasErrors()) {
            var loadingErrors = getLoadingErrors();
            for (var loadingError : loadingErrors) {
                LOGGER.fatal(CORE, "Error during pre-loading phase: {}", FMLTranslations.translateIssueEnglish(loadingError), loadingError.cause());
            }
            if (modList != null) {
                cancelLoading(modList);
            }
            throw new ModLoadingException(loadingIssues);
        }
    }

    private static void cancelLoading(ModList modList) {
        StartupNotificationManager.modLoaderMessage("ERROR DURING MOD LOADING");
        modList.setLoadedMods(Collections.emptyList());
    }

    private static void constructMods(Executor syncExecutor, Executor parallelExecutor, Runnable periodicTask) {
        var workQueue = new DeferredWorkQueue("Mod Construction");
        dispatchParallelTask("Mod Construction", parallelExecutor, periodicTask, modContainer -> {
            modContainer.constructMod();
            modContainer.acceptEvent(new FMLConstructModEvent(modContainer, workQueue));
        });
        waitForTask("Mod Construction: Deferred Queue", periodicTask, CompletableFuture.runAsync(workQueue::runTasks, syncExecutor));
    }

    /**
     * Runs a single task on the {@code syncExecutor}, while ticking the loading screen.
     */
    public static void runInitTask(String name, Executor syncExecutor, Runnable periodicTask, Runnable initTask) {
        waitForTask(name, periodicTask, CompletableFuture.runAsync(initTask, syncExecutor));
    }

    /**
     * Dispatches a parallel event across all mod containers, with progress displayed on the loading screen.
     */
    public static void dispatchParallelEvent(String name, Executor syncExecutor, Executor parallelExecutor, Runnable periodicTask, BiFunction<ModContainer, DeferredWorkQueue, ParallelDispatchEvent> eventConstructor) {
        var workQueue = new DeferredWorkQueue(name);
        dispatchParallelTask(name, parallelExecutor, periodicTask, modContainer -> {
            modContainer.acceptEvent(eventConstructor.apply(modContainer, workQueue));
        });
        runInitTask(name + ": Deferred Queue", syncExecutor, periodicTask, workQueue::runTasks);
    }

    /**
     * Waits for a task to complete, displaying the name of the task on the loading screen.
     */
    public static void waitForTask(String name, Runnable periodicTask, CompletableFuture<?> future) {
        var progress = StartupNotificationManager.addProgressBar(name, 0);
        try {
            waitForFuture(name, periodicTask, future);
        } finally {
            progress.complete();
        }
    }

    /**
     * Exception that is fired when a mod loading future cannot be executed because a dependent future failed.
     * It is only used for control flow and easy filtering out, but never logged or propagated further.
     */
    private static class DependentFutureFailedException extends RuntimeException {}

    /**
     * Dispatches a task across all mod containers in parallel, with progress displayed on the loading screen.
     */
    public static void dispatchParallelTask(String name, Executor parallelExecutor, Runnable periodicTask, Consumer<ModContainer> task) {
        var progress = StartupNotificationManager.addProgressBar(name, modList.size());
        try {
            periodicTask.run();
            Map<IModInfo, CompletableFuture<Void>> modFutures = new IdentityHashMap<>(modList.size());
            var futureList = modList.getSortedMods().stream()
                    .map(modContainer -> {
                        // Collect futures for all dependencies first
                        var depFutures = FMLLoader.getCurrent().getLoadingModList().getDependencies(modContainer.getModInfo()).stream()
                                .map(modInfo -> {
                                    var future = modFutures.get(modInfo);
                                    if (future == null) {
                                        throw new IllegalStateException("Dependency future for mod %s which is a dependency of %s not found!".formatted(
                                                modInfo.getModId(), modContainer.getModId()));
                                    }
                                    return future;
                                })
                                .toArray(CompletableFuture[]::new);

                        // Build the future for this container
                        var future = CompletableFuture.allOf(depFutures)
                                .<Void>handleAsync((void_, exception) -> {
                                    if (exception != null) {
                                        // If there was any exception, short circuit.
                                        // The exception will already be handled by `waitForFuture` since it comes from another mod.
                                        LOGGER.debug("Skipping {} task for mod {} because a dependency threw an exception.", name, modContainer.getModId());
                                        progress.increment();
                                        // Throw a marker exception to make sure that dependencies of *this* task don't get executed.
                                        throw new DependentFutureFailedException();
                                    }

                                    try {
                                        ModLoadingContext.get().setActiveContainer(modContainer);
                                        task.accept(modContainer);
                                    } finally {
                                        progress.increment();
                                        ModLoadingContext.get().setActiveContainer(null);
                                    }
                                    return null;
                                }, parallelExecutor);
                        modFutures.put(modContainer.getModInfo(), future);
                        return future;
                    })
                    .toList();
            var singleFuture = ModList.gather(futureList)
                    .thenCompose(ModList::completableFutureFromExceptionList);
            waitForFuture(name, periodicTask, singleFuture);
        } finally {
            progress.complete();
        }
    }

    private static void waitForFuture(String name, Runnable periodicTask, CompletableFuture<?> future) {
        while (true) {
            periodicTask.run();
            try {
                future.get(50, TimeUnit.MILLISECONDS);
                return;
            } catch (ExecutionException e) {
                // Merge all potential modloading issues
                var issueCountBefore = loadingIssues.size();
                // Add the cause itself if it seems meaningful based on its type and/or message, since we'd
                // present this exception to the user using toString().
                // "RuntimeException: null" or "IllegalStateException: null" isn't meaningful.
                if (isMeaningfulException(e.getCause())) {
                    addLoadingIssuesFromException(name, e.getCause());
                }
                for (var error : e.getCause().getSuppressed()) {
                    // When a prerequisite of the task failed due to an exception, skip this so only
                    // the root cause is reported.
                    if (!(error instanceof DependentFutureFailedException)) {
                        addLoadingIssuesFromException(name, error);
                    }
                }
                // If we discarded all exceptions, we'd report an empty issue list while still canceling the loading process
                // Fall back to using the cause.
                if (loadingIssues.isEmpty()) {
                    addLoadingIssuesFromException(name, e.getCause());
                }

                var errorCount = loadingIssues.size() - issueCountBefore;
                LOGGER.fatal(LOADING, "Failed to wait for future {}, {} errors found", name, errorCount);
                cancelLoading(modList);
                throw new ModLoadingException(loadingIssues);
            } catch (Exception ignored) {}
        }
    }

    private static boolean isMeaningfulException(Throwable error) {
        var message = error.getLocalizedMessage();
        if (message != null && !message.isBlank()) {
            return true; // We'd consider any exception with a message as meaningful
        }
        // The trifecta of generic exceptions...
        return !error.getClass().isAssignableFrom(RuntimeException.class)
                && !error.getClass().isAssignableFrom(IllegalStateException.class)
                && !error.getClass().isAssignableFrom(IllegalArgumentException.class);
    }

    private static void addLoadingIssuesFromException(String context, Throwable error) {
        if (error instanceof ModLoadingException modLoadingException) {
            loadingIssues.addAll(modLoadingException.getIssues());
        } else {
            loadingIssues.add(ModLoadingIssue.error("fml.modloadingissue.uncaughterror", context).withCause(error));
        }
    }

    private static List<ModContainer> buildMods(IModFile modFile) {
        Map<IModLanguageLoader, Set<ModContainer>> byLoader = new IdentityHashMap<>();
        var containers = modFile.getModFileInfo()
                .getMods()
                .stream()
                .map(info -> {
                    var container = buildModContainerFromTOML(info, modFile.getScanResult());
                    var cont = byLoader.computeIfAbsent(info.getLoader(), k -> new HashSet<>());
                    if (container != null) cont.add(container);
                    return container;
                })
                .filter(Objects::nonNull)
                .toList();
        byLoader.forEach((loader, loaded) -> loader.validate(modFile, loaded, ModLoader::addLoadingIssue));
        return containers;
    }

    private static ModContainer buildModContainerFromTOML(IModInfo modInfo, ModFileScanData scanData) {
        try {
            return modInfo.getLoader().loadMod(modInfo, scanData, FMLLoader.getCurrent().getGameLayer());
        } catch (ModLoadingException mle) {
            // exceptions are caught and added to the error list for later handling
            loadingIssues.addAll(mle.getIssues());
            // return a null container here because we tried and failed building a container.
            return null;
        }
    }

    public static <T extends Event & IModBusEvent> void runEventGenerator(Function<ModContainer, T> generator) {
        if (hasErrors()) {
            LOGGER.error("Cowardly refusing to send event generator to a broken mod state");
            return;
        }

        // Construct events
        List<ModContainer> modContainers = ModList.get().getSortedMods();
        List<T> events = new ArrayList<>(modContainers.size());
        ModList.get().forEachModInOrder(mc -> events.add(generator.apply(mc)));

        // Post them
        for (EventPriority phase : EventPriority.values()) {
            for (int i = 0; i < modContainers.size(); i++) {
                modContainers.get(i).acceptEvent(phase, events.get(i));
            }
        }
    }

    public static <T extends Event & IModBusEvent> void postEvent(T e) {
        if (hasErrors()) {
            LOGGER.error("Cowardly refusing to send event {} to a broken mod state", e.getClass().getName());
            return;
        }
        for (EventPriority phase : EventPriority.values()) {
            ModList.get().forEachModInOrder(mc -> mc.acceptEvent(phase, e));
        }
    }

    public static <T extends Event & IModBusEvent> T postEventWithReturn(T e) {
        postEvent(e);
        return e;
    }

    public static <T extends Event & IModBusEvent> void postEventWrapContainerInModOrder(T event) {
        postEventWithWrapInModOrder(event, (mc, e) -> ModLoadingContext.get().setActiveContainer(mc), (mc, e) -> ModLoadingContext.get().setActiveContainer(null));
    }

    public static <T extends Event & IModBusEvent> void postEventWithWrapInModOrder(T e, BiConsumer<ModContainer, T> pre, BiConsumer<ModContainer, T> post) {
        if (hasErrors()) {
            LOGGER.error("Cowardly refusing to send event {} to a broken mod state", e.getClass().getName());
            return;
        }
        for (EventPriority phase : EventPriority.values()) {
            ModList.get().forEachModInOrder(mc -> {
                pre.accept(mc, e);
                mc.acceptEvent(phase, e);
                post.accept(mc, e);
            });
        }
    }

    /**
     * @return If the errors occurred during mod loading. Use if you interact with vanilla systems directly during loading
     *         and don't want to cause extraneous crashes due to trying to do things that aren't possible.
     *         If you are running in a Mixin before mod loading has actually started, check {@link LoadingModList#hasErrors()} instead.
     */
    public static boolean hasErrors() {
        return !loadingIssues.isEmpty() && loadingIssues.stream().anyMatch(issue -> issue.severity() == ModLoadingIssue.Severity.ERROR);
    }

    @ApiStatus.Internal
    public static List<ModLoadingIssue> getLoadingErrors() {
        return loadingIssues.stream().filter(issue -> issue.severity() == ModLoadingIssue.Severity.ERROR).toList();
    }

    @ApiStatus.Internal
    public static List<ModLoadingIssue> getLoadingWarnings() {
        return loadingIssues.stream().filter(issue -> issue.severity() == ModLoadingIssue.Severity.WARNING).toList();
    }

    @ApiStatus.Internal
    public static List<ModLoadingIssue> getLoadingIssues() {
        return List.copyOf(loadingIssues);
    }

    @VisibleForTesting
    @ApiStatus.Internal
    public static void clearLoadingIssues() {
        LOGGER.info("Clearing {} loading issues", loadingIssues.size());
        loadingIssues.clear();
    }

    @ApiStatus.Internal
    public static void addLoadingIssue(ModLoadingIssue issue) {
        loadingIssues.add(issue);
    }

    @VisibleForTesting
    @ApiStatus.Internal
    public static void clear() {
        LOGGER.info("Clearing ModLoader");
        loadingIssues.clear();
        modList = null;
    }
}
