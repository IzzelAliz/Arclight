/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.jarcontents;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * Access to the contents of a list of {@link Path}s, interpreted as a jar file.
 * <p>
 * <h2>Relative Paths</h2>
 * To address files within jars, paths that are interpreted to be relative to the root of the jar file are used.
 * The relative path for a class-file for class {@code your.package.YourClass} would be {@code your/package/YourClass},
 * for example.
 */
@ApiStatus.NonExtendable
public interface JarContents extends Closeable {
    @FunctionalInterface
    interface PathFilter {
        boolean test(String relativePath);

        @Nullable
        static PathFilter and(@Nullable PathFilter a, @Nullable PathFilter b) {
            if (a == null) {
                return b;
            } else if (b == null) {
                return a;
            } else {
                return relativePath -> a.test(relativePath) && b.test(relativePath);
            }
        }
    }

    record FilteredPath(Path path, @Nullable CompositeJarContents.PathFilter filter) {
        public FilteredPath(Path path) {
            this(path, null);
        }
    }

    /**
     * Creates jar contents from paths with optional per-path filters.
     * <p>Non-existent paths are ignored. If all paths are missing, throws {@link NoSuchFileException}.
     * <p>If only one valid unfiltered path is provided, behaves as {@link #ofPath(Path)}.
     */
    static JarContents ofFilteredPaths(Collection<FilteredPath> paths) throws IOException {
        if (paths.isEmpty()) {
            throw new IllegalArgumentException("Cannot construct jar contents without any paths.");
        } else {
            List<JarContents> contents = new ArrayList<>(paths.size());
            List<PathFilter> filters = new ArrayList<>(paths.size());
            for (var filteredPath : paths) {
                if (Files.exists(filteredPath.path)) {
                    contents.add(ofPath(filteredPath.path));
                    filters.add(filteredPath.filter);
                }
            }

            if (contents.isEmpty()) {
                String pathList = paths.stream().map(f -> f.path.toString()).collect(Collectors.joining(", "));
                throw new NoSuchFileException("At least one of the paths must exist when constructing jar contents: " + pathList);
            } else if (contents.size() == 1 && filters.getFirst() == null) {
                return contents.getFirst(); // Uncommon case, but we'll still optimize for it
            }

            return new CompositeJarContents(contents, filters);
        }
    }

    /**
     * Creates jar contents from multiple paths, treating them as overlay layers.
     * <p>Later paths override earlier ones for conflicting files.
     * <p>Non-existent paths are ignored. If all paths are missing, throws {@link NoSuchFileException}.
     * <p>If only one valid path is provided, behaves as {@link #ofPath(Path)}.
     */
    static JarContents ofPaths(Collection<Path> paths) throws IOException {
        if (paths.isEmpty()) {
            throw new IllegalArgumentException("Cannot construct jar contents without any paths.");
        } else {
            List<JarContents> contents = new ArrayList<>(paths.size());
            for (var path : paths) {
                if (Files.exists(path)) {
                    contents.add(ofPath(path));
                }
            }

            if (contents.isEmpty()) {
                throw new NoSuchFileException("At least one of the paths must exist when constructing jar contents: " + paths);
            } else if (contents.size() == 1) {
                return contents.getFirst();
            }

            return new CompositeJarContents(contents);
        }
    }

    /**
     * Creates jar contents from a single path.
     * <p>The path must exist and be either a regular file (treated as a jar/zip) or a directory.
     */
    static JarContents ofPath(Path path) throws IOException {
        if (Files.isRegularFile(path)) {
            return new JarFileContents(path);
        } else if (Files.isDirectory(path)) {
            return new FolderJarContents(path);
        } else {
            throw new NoSuchFileException("Cannot construct mod container from missing " + path);
        }
    }

    /**
     * Creates an empty jar contents instance.
     * <p>The path is used only as an identifier and need not exist.
     * <p>All content queries will return empty/null results.
     */
    static JarContents empty(Path path) {
        return new EmptyJarContents(path);
    }

    /**
     * {@return SHA-256 hash of the contents, if available}
     * <p>This value is intended to be used as a cache-key, and
     * will be empty if the jar contents are not stable enough to be cached (i.e. they're sourced from memory or a folder).
     */
    Optional<String> getChecksum();

    Path getPrimaryPath();

    /**
     * Returns the locations that this Jar content was opened from.
     * <p>Usually this will only contain a single path, for example to the Jar file that was opened, but especially during development, it can contain multiple build output folders that were joined into a single virtual Jar file.
     * <p>The resulting paths do not need to be on the local file-system, they can be from custom NIO filesystem implementations.
     * <p>The returned list may also not contain all content roots if the underlying jar content is held in-memory.
     */
    Collection<Path> getContentRoots();

    /**
     * Tries to find a resource with the given path in this jar content.
     *
     * @param relativePath See {@link JarContents} for a definition of relative paths.
     * @return Null if the resource could not be found within the jar, or if {@code relativePath} refers to a directory.
     */
    @Nullable
    JarResource get(String relativePath);

    /**
     * Looks for a file in the jar.
     *
     * @return A URI for the file, or empty if the file cannot be found or if {@code relativePath} refers to a directory.
     */
    Optional<URI> findFile(String relativePath);

    /**
     * Tries to open a file inside the jar content using a path relative to the root.
     * <p>The stream will not be buffered.
     * <p>The behavior when {@code relativePath} refers to a directory rather than a file is unspecified. The
     * method may throw a {@code IOException} immediately, but may also defer this until the first byte is
     * read from the stream. This behavior is filesystem provider specific.
     *
     * @return null if the file cannot be found
     */
    @Nullable
    InputStream openFile(String relativePath) throws IOException;

    /**
     * A convenience method that {@linkplain #openFile(String) opens a file} and if the file was found,
     * returns its content.
     * <p>Trying to read the contents of a directory using this method will throw an {@code IOException}.
     *
     * @return Null if the file does not exist.
     */
    default byte @Nullable [] readFile(String relativePath) throws IOException {
        try (var in = openFile(relativePath)) {
            if (in == null) {
                return null;
            }
            return in.readAllBytes();
        }
    }

    /**
     * Checks, if a given file exists in this jar.
     *
     * @param relativePath The path to the file, relative to the root of this Jar file.
     * @return True if the file exists, false if it doesn't or {@code relativePath} refers to a directory.
     */
    boolean containsFile(String relativePath);

    /**
     * {@return the manifest of the jar}
     * Empty if no manifest is present in the jar.
     * <p><strong>NOTE:</strong> Do not modify the returned manifest.
     */
    Manifest getManifest();

    /**
     * Visits all content found in this jar.
     */
    default void visitContent(JarResourceVisitor visitor) {
        visitContent("", visitor);
    }

    /**
     * Visits all content found in this jar, starting in the given folder.
     * <p>If the folder does not exist, the visitor is not invoked and no error is raised.
     */
    void visitContent(String startingFolder, JarResourceVisitor visitor);
}
