/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.jarcontents;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import net.neoforged.fml.util.PathPrettyPrinting;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

@ApiStatus.Internal
public final class JarFileContents implements JarContents {
    private final Path path;
    private final JarFile jarFile;
    private final Manifest jarManifest;

    public JarFileContents(Path path) throws IOException {
        this.path = path;
        this.jarFile = new JarFile(path.toFile(), true, JarFile.OPEN_READ, JarFile.runtimeVersion());
        this.jarManifest = Objects.requireNonNullElse(this.jarFile.getManifest(), EmptyManifest.INSTANCE);
    }

    @Override
    public Path getPrimaryPath() {
        return path;
    }

    @Override
    public Collection<Path> getContentRoots() {
        return List.of(path);
    }

    @Override
    public Optional<String> getChecksum() {
        try (var in = Files.newInputStream(path)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            DigestInputStream digestIn = new DigestInputStream(in, digest);
            digestIn.transferTo(OutputStream.nullOutputStream());
            return Optional.of(HexFormat.of().formatHex(digest.digest()));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to compute checksum for " + path, e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Standard JCA algorithm is missing.", e);
        }
    }

    @Override
    public String toString() {
        return "jar(" + PathPrettyPrinting.prettyPrint(path) + ")";
    }

    @Nullable
    @Override
    public Manifest getManifest() {
        return jarManifest;
    }

    @Override
    public Optional<URI> findFile(String relativePath) {
        relativePath = PathNormalization.normalize(relativePath);

        var entry = jarFile.getEntry(relativePath);
        if (entry != null && !entry.isDirectory()) {
            return Optional.of(URI.create("jar:" + path.toUri() + "!/" + relativePath));
        }
        return Optional.empty();
    }

    @Override
    public JarResource get(String relativePath) {
        relativePath = PathNormalization.normalize(relativePath);

        var entry = jarFile.getJarEntry(relativePath);
        if (entry == null || entry.isDirectory()) {
            return null;
        }
        return new JarEntryResource(entry, false);
    }

    @Override
    public boolean containsFile(String relativePath) {
        relativePath = PathNormalization.normalize(relativePath);
        var entry = jarFile.getEntry(relativePath);
        return entry != null && !entry.isDirectory();
    }

    @Override
    public InputStream openFile(String relativePath) throws IOException {
        relativePath = PathNormalization.normalize(relativePath);

        if (relativePath.isEmpty()) {
            throw new IOException("The path refers to the root directory");
        }

        var entry = jarFile.getEntry(relativePath);
        if (entry != null) {
            if (entry.isDirectory()) {
                throw new IOException("The path " + relativePath + " refers to a directory");
            }
            return jarFile.getInputStream(entry);
        }
        return null;
    }

    @Override
    public byte[] readFile(String relativePath) throws IOException {
        relativePath = PathNormalization.normalize(relativePath);
        if (relativePath.isEmpty()) {
            throw new IOException("The path refers to the root directory");
        }

        var entry = jarFile.getEntry(relativePath);
        if (entry != null) {
            if (entry.isDirectory()) {
                throw new IOException("The path " + relativePath + " refers to a directory");
            }
            try (var input = jarFile.getInputStream(entry)) {
                // TODO in theory we can at least use entry uncompressed size as a hint here
                return input.readAllBytes();
            }
        }
        return null;
    }

    @Override
    public void visitContent(String startingFolder, JarResourceVisitor visitor) {
        startingFolder = PathNormalization.normalizeFolderPrefix(startingFolder);

        var resource = new JarEntryResource(null, true);
        var it = jarFile.entries().asIterator();
        while (it.hasNext()) {
            var entry = it.next();
            if (entry.isDirectory()) {
                continue;
            }

            if (!startingFolder.isEmpty() && !entry.getName().startsWith(startingFolder)) {
                continue;
            }

            var relativePath = PathNormalization.normalize(entry.getName());
            resource.entry = entry;
            visitor.visit(relativePath, resource);
        }
    }

    @Override
    public void close() {
        try {
            jarFile.close();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to close ZIP-File " + path, e);
        }
    }

    private final class JarEntryResource implements JarResource {
        private final boolean mutable;
        private JarEntry entry;

        public JarEntryResource(JarEntry entry, boolean mutable) {
            this.entry = entry;
            this.mutable = mutable;
        }

        @Override
        public InputStream open() throws IOException {
            return jarFile.getInputStream(entry);
        }

        @Override
        public JarResourceAttributes attributes() {
            return new JarResourceAttributes(entry.getLastModifiedTime(), entry.getSize());
        }

        @Override
        public JarResource retain() {
            if (mutable) {
                return new JarEntryResource(entry, false);
            } else {
                return this;
            }
        }
    }
}
