/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.loading;

import static net.neoforged.fml.loading.LogMarkers.CORE;

import com.mojang.logging.LogUtils;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import org.jetbrains.annotations.ApiStatus;
import org.slf4j.Logger;

public enum FMLPaths {
    GAMEDIR(),
    JIJ_CACHEDIR(".cache/jij"),
    MODSDIR("mods"),
    CONFIGDIR("config"),
    FMLCONFIG(false, CONFIGDIR, "fml.toml");

    private static final Logger LOGGER = LogUtils.getLogger();
    private final Path relativePath;
    private final boolean isDirectory;
    private Path absolutePath;

    FMLPaths() {
        this("");
    }

    FMLPaths(String... path) {
        relativePath = computePath(path);
        this.isDirectory = true;
    }

    FMLPaths(boolean isDir, FMLPaths parent, String... path) {
        this.relativePath = parent.relativePath.resolve(computePath(path));
        this.isDirectory = isDir;
    }

    private Path computePath(String... path) {
        return Paths.get(path[0], Arrays.copyOfRange(path, 1, path.length));
    }

    @ApiStatus.Internal
    public static void loadAbsolutePaths(Path rootPath) {
        for (FMLPaths path : FMLPaths.values()) {
            path.absolutePath = rootPath.resolve(path.relativePath).toAbsolutePath().normalize();
            if (path.isDirectory && !Files.isDirectory(path.absolutePath)) {
                try {
                    Files.createDirectories(path.absolutePath);
                } catch (IOException ioe) {
                    throw new UncheckedIOException(ioe);
                }
            }
            if (LOGGER.isDebugEnabled(CORE)) {
                LOGGER.debug(CORE, "Path {} is {}", path, path.absolutePath);
            }
        }
    }

    public static Path getOrCreateGameRelativePath(Path path) {
        Path gameFolderPath = FMLPaths.GAMEDIR.get().resolve(path);

        if (!Files.isDirectory(gameFolderPath)) {
            try {
                Files.createDirectories(gameFolderPath);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return gameFolderPath;
    }

    public Path relative() {
        return relativePath;
    }

    public Path get() {
        return absolutePath;
    }
}
