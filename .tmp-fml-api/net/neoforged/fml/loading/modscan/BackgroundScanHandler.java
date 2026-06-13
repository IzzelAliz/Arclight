/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.loading.modscan;

import com.mojang.logging.LogUtils;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import net.neoforged.fml.loading.FMLConfig;
import net.neoforged.fml.loading.LogMarkers;
import net.neoforged.fml.loading.moddiscovery.ModFile;
import net.neoforged.neoforgespi.locating.IModFile;
import org.slf4j.Logger;

public class BackgroundScanHandler {
    private enum ScanStatus {
        NOT_STARTED,
        RUNNING,
        COMPLETE,
        TIMED_OUT,
        INTERRUPTED,
        ERRORED
    }

    private static final Logger LOGGER = LogUtils.getLogger();
    private final ExecutorService modContentScanner;
    private ScanStatus status;

    public BackgroundScanHandler(Collection<IModFile> modFiles) {
        int maxThreads = FMLConfig.getIntConfigValue(FMLConfig.ConfigValue.MAX_THREADS);
        // Leave 1 thread for Minecraft's own bootstrap
        int poolSize = Math.max(1, maxThreads - 1);
        AtomicInteger threadCount = new AtomicInteger();
        modContentScanner = Executors.newFixedThreadPool(poolSize, r -> {
            Thread thread = Executors.defaultThreadFactory().newThread(r);
            thread.setDaemon(true);
            thread.setName("background-scan-handler-" + threadCount.getAndIncrement());
            return thread;
        });
        status = ScanStatus.NOT_STARTED;

        if (modContentScanner.isShutdown()) {
            status = ScanStatus.ERRORED;
            throw new IllegalStateException("Scanner has shutdown");
        }

        status = ScanStatus.RUNNING;
        for (var modFile : modFiles) {
            ((ModFile) modFile).startScan(modContentScanner)
                    .whenComplete((ignored, t) -> this.logFailure(modFile, t));
        }
    }

    private synchronized void logFailure(IModFile file, Throwable throwable) {
        if (throwable != null) {
            status = ScanStatus.ERRORED;
            LOGGER.error(LogMarkers.SCAN, "An error occurred scanning file {}", file, throwable);
        }
    }

    public void waitForScanToComplete(Runnable ticker) {
        boolean timeoutActive = System.getProperty("fml.disableScanTimeout") == null;
        Instant deadline = Instant.now().plus(Duration.ofMinutes(10));
        modContentScanner.shutdown();
        do {
            ticker.run();
            try {
                status = modContentScanner.awaitTermination(50, TimeUnit.MILLISECONDS) ? ScanStatus.COMPLETE : ScanStatus.RUNNING;
            } catch (InterruptedException e) {
                status = ScanStatus.INTERRUPTED;
            }
            if (timeoutActive && Instant.now().isAfter(deadline)) status = ScanStatus.TIMED_OUT;
        } while (status == ScanStatus.RUNNING);
        if (status == ScanStatus.INTERRUPTED) Thread.currentThread().interrupt();
        if (status != ScanStatus.COMPLETE) throw new IllegalStateException("Failed to complete mod scan");
    }
}
