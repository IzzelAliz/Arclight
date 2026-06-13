/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.loading;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import net.neoforged.fml.ModLoadingIssue;
import net.neoforged.fml.util.ServiceLoaderUtil;
import net.neoforged.neoforgespi.ILaunchContext;
import net.neoforged.neoforgespi.earlywindow.GraphicsBootstrapper;
import net.neoforged.neoforgespi.earlywindow.ImmediateWindowProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

@ApiStatus.Internal
public class ImmediateWindowHandler {
    private static final Logger LOGGER = LogManager.getLogger();

    @Nullable
    static ImmediateWindowProvider provider;

    public static void load(ILaunchContext context, boolean headless, ProgramArgs arguments) {
        ServiceLoaderUtil.loadEarlyServices(context, GraphicsBootstrapper.class, List.of())
                .forEach(bootstrap -> {
                    LOGGER.info("Running graphics bootstrap plugin {}", bootstrap.name());
                    bootstrap.bootstrap(arguments.getArguments()); // TODO: Should take ProgramArgs so it can *remove* args
                });

        if (headless) {
            provider = null;
            LOGGER.info("Not loading early display in headless mode.");
            return;
        }

        if (!FMLConfig.getBoolConfigValue(FMLConfig.ConfigValue.EARLY_WINDOW_CONTROL)) {
            provider = null;
            LOGGER.info("ImmediateWindowProvider not loading because splash screen is disabled");
        } else {
            var providername = FMLConfig.getConfigValue(FMLConfig.ConfigValue.EARLY_WINDOW_PROVIDER);
            LOGGER.info("Loading ImmediateWindowProvider {}", providername);
            var maybeProvider = ServiceLoaderUtil.loadEarlyServices(context, ImmediateWindowProvider.class, List.of())
                    .stream()
                    .filter(p -> Objects.equals(p.name(), providername))
                    .findFirst();
            provider = maybeProvider.orElse(null);
            if (provider == null) {
                LOGGER.info("Failed to find ImmediateWindowProvider {}, disabling", providername);
            } else {
                try {
                    provider.initialize(arguments);
                } catch (Exception e) {
                    LOGGER.error("Failed to initialize ImmediateWindowProvider '{}'", providername, e);
                    provider = null;
                }
            }
        }
        // Only update config if the provider isn't the dummy provider
        if (provider != null) {
            FMLConfig.updateConfig(FMLConfig.ConfigValue.EARLY_WINDOW_PROVIDER, provider.name());
        }
    }

    public static void setNeoForgeVersion(String version) {
        if (provider != null) {
            provider.setNeoForgeVersion(version);
        }
    }

    public static void setMinecraftVersion(String version) {
        if (provider != null) {
            provider.setMinecraftVersion(version);
        }
    }

    public static void renderTick() {
        if (provider != null) {
            provider.periodicTick();
        }
    }

    public static void updateProgress(String message) {
        if (provider != null) {
            provider.updateProgress(message);
        }
    }

    public static void crash(String message) {
        if (provider != null) {
            provider.crash(message);
        }
    }

    public static void displayFatalErrorAndExit(List<ModLoadingIssue> issues, @Nullable Path modsFolder, @Nullable Path logFile, @Nullable Path crashReportFile) {
        if (provider != null) {
            provider.displayFatalErrorAndExit(issues, modsFolder, logFile, crashReportFile);
        }
    }
}
