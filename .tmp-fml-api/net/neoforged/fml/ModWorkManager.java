/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml;

import static net.neoforged.fml.Logging.LOADING;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import net.neoforged.fml.loading.FMLConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ModWorkManager {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final Executor syncWorkExecutor = Executors.newSingleThreadExecutor(r -> {
        var thread = new Thread(r, "modloading-sync-worker");
        thread.setDaemon(true);
        return thread;
    });

    /**
     * Executor that runs tasks on a single thread in the order they are submitted.
     */
    public static Executor syncExecutor() {
        return syncWorkExecutor;
    }

    private static ForkJoinPool parallelThreadPool;

    /**
     * Executor that runs tasks in parallel across multiple background threads.
     */
    public static Executor parallelExecutor() {
        if (parallelThreadPool == null) {
            int loadingThreadCount = FMLConfig.getIntConfigValue(FMLConfig.ConfigValue.MAX_THREADS);
            LOGGER.debug(LOADING, "Using {} threads for parallel mod-loading", loadingThreadCount);
            parallelThreadPool = new ForkJoinPool(loadingThreadCount, ModWorkManager::newForkJoinWorkerThread, null, false);
        }
        return parallelThreadPool;
    }

    private static ForkJoinWorkerThread newForkJoinWorkerThread(ForkJoinPool pool) {
        ForkJoinWorkerThread thread = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool);
        thread.setName("modloading-worker-" + thread.getPoolIndex());
        // The default sets it to the SystemClassloader, so copy the current one.
        thread.setContextClassLoader(Thread.currentThread().getContextClassLoader());
        return thread;
    }
}
