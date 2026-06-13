/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.jarcontents;

import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.jar.Manifest;
import net.neoforged.fml.util.PathPrettyPrinting;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * An immutable, empty jar content, which is identified by a {@link Path}, which may or may not exist.
 */
@ApiStatus.Internal
public final class EmptyJarContents implements JarContents {
    private final Path path;

    /**
     * @see JarContents#empty(Path)
     */
    public EmptyJarContents(Path path) {
        this.path = Objects.requireNonNull(path, "path");
    }

    @Override
    public Optional<String> getChecksum() {
        return Optional.empty();
    }

    @Override
    public Optional<URI> findFile(String relativePath) {
        return Optional.empty();
    }

    @Override
    public @Nullable JarResource get(String relativePath) {
        return null;
    }

    @Override
    public boolean containsFile(String relativePath) {
        return false;
    }

    @Override
    public Path getPrimaryPath() {
        return path;
    }

    @Override
    public Collection<Path> getContentRoots() {
        return List.of();
    }

    @Override
    public Manifest getManifest() {
        return EmptyManifest.INSTANCE;
    }

    @Override
    public @Nullable InputStream openFile(String relativePath) {
        return null;
    }

    @Override
    public byte @Nullable [] readFile(String relativePath) {
        return null;
    }

    @Override
    public void visitContent(String startingFolder, JarResourceVisitor visitor) {}

    @Override
    public void close() {}

    @Override
    public String toString() {
        return "empty(" + PathPrettyPrinting.prettyPrint(path) + ")";
    }
}
