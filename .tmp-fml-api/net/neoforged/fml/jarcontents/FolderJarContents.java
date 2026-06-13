/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.jarcontents;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributeView;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import net.neoforged.fml.util.PathPrettyPrinting;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

@ApiStatus.Internal
public final class FolderJarContents implements JarContents {
    private final Path path;
    private final Object manifestLock = new Object();
    private Manifest cachedManifest;

    public FolderJarContents(Path path) {
        this.path = path;
    }

    @Override
    public Path getPrimaryPath() {
        return path;
    }

    @Override
    public Optional<String> getChecksum() {
        return Optional.empty();
    }

    @Override
    public @Nullable JarResource get(String relativePath) {
        var path = fromRelativePath(relativePath);
        if (Files.isRegularFile(path)) {
            return new FileResource(path, false);
        }
        return null;
    }

    @Override
    public boolean containsFile(String relativePath) {
        return Files.isRegularFile(fromRelativePath(relativePath));
    }

    @Override
    public InputStream openFile(String relativePath) throws IOException {
        try {
            return Files.newInputStream(fromRelativePath(relativePath));
        } catch (NoSuchFileException e) {
            return null;
        }
    }

    @Override
    public byte[] readFile(String relativePath) throws IOException {
        try {
            return Files.readAllBytes(fromRelativePath(relativePath));
        } catch (NoSuchFileException e) {
            return null;
        }
    }

    @Override
    public Collection<Path> getContentRoots() {
        return List.of(path);
    }

    @Override
    public void visitContent(String startingFolder, JarResourceVisitor visitor) {
        var startingPoint = getVisitStartingPoint(startingFolder);
        if (!startingPoint.startsWith(path)) {
            return; // Don't allow ../ escapes
        }
        if (!Files.isDirectory(startingPoint)) {
            return;
        }

        try (var stream = Files.walk(startingPoint)) {
            var locatedResource = new FileResource(null, true);
            stream.forEach(path -> {
                if (Files.isRegularFile(path)) {
                    var relativePath = PathNormalization.normalize(this.path.relativize(path).toString());
                    locatedResource.path = path;
                    visitor.visit(relativePath, locatedResource);
                }
            });
        } catch (NoSuchFileException ignored) {
            // The specific subfolder doesn't exist
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to walk contents of " + this, e);
        }
    }

    private Path getVisitStartingPoint(String startingFolder) {
        startingFolder = PathNormalization.normalize(startingFolder);

        var startingPoint = path;
        if (!startingFolder.isEmpty()) {
            startingPoint = path.resolve(startingFolder).normalize();
        }
        return startingPoint;
    }

    @Override
    public Optional<URI> findFile(String relativePath) {
        var pathToFile = fromRelativePath(relativePath);
        return Files.isRegularFile(pathToFile) ? Optional.of(pathToFile.toUri()) : Optional.empty();
    }

    @Override
    public Manifest getManifest() {
        var manifest = cachedManifest;
        if (manifest == null) {
            synchronized (manifestLock) {
                manifest = cachedManifest;
                if (manifest == null) {
                    var manifestFile = path.resolve(JarFile.MANIFEST_NAME);
                    try (var in = Files.newInputStream(manifestFile)) {
                        manifest = new Manifest(in);
                    } catch (NoSuchFileException ignored) {
                        manifest = EmptyManifest.INSTANCE;
                    } catch (IOException e) {
                        throw new UncheckedIOException("Failed to read manifest " + manifestFile, e);
                    }
                    cachedManifest = manifest;
                }
            }
        }
        return manifest;
    }

    @Override
    public void close() {}

    @Override
    public String toString() {
        return "folder(" + PathPrettyPrinting.prettyPrint(path) + ")";
    }

    private Path fromRelativePath(String relativePath) {
        // Checking for normalization here prevents path escapes
        relativePath = PathNormalization.normalize(relativePath);
        return path.resolve(relativePath);
    }

    private static class FileResource implements JarResource {
        private final boolean mutable;
        private Path path;

        public FileResource(Path path, boolean mutable) {
            this.path = path;
            this.mutable = mutable;
        }

        @Override
        public InputStream open() throws IOException {
            return Files.newInputStream(path);
        }

        @Override
        public JarResourceAttributes attributes() throws IOException {
            var attributes = Files.getFileAttributeView(path, BasicFileAttributeView.class).readAttributes();
            return new JarResourceAttributes(attributes.lastModifiedTime(), attributes.size());
        }

        @Override
        public JarResource retain() {
            if (mutable) {
                return new FileResource(path, false);
            } else {
                return this;
            }
        }
    }
}
