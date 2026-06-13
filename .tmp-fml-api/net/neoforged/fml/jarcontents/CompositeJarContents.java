/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.jarcontents;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * A composite {@link JarContents} implementation which combines the contents of other jar contents transparently,
 * and optionally applies filtering.
 */
@ApiStatus.Internal
public final class CompositeJarContents implements JarContents {
    private final JarContents[] delegates;
    @Nullable
    private final PathFilter @Nullable [] filters;
    private final List<Path> contentRoots;

    private volatile Optional<String> checksum;

    /**
     * Constructs a new composite jar contents without filtering.
     *
     * @see JarContents#ofFilteredPaths
     * @see JarContents#ofPaths
     */
    public CompositeJarContents(List<JarContents> delegates) {
        this(delegates, null);
    }

    /**
     * Constructs a composite jar contents object with optional filtering.
     *
     * @param delegates The delegates that are combined. The delegation is last to first, which means the content in later delegates takes priority.
     * @param filters   Optional filters for each entry in {@code delegates}. If {@code null}, no filtering is applied at all.
     *                  If the filter for a delegate is {@code null}, no filtering is applied to that delegate.
     * @see JarContents#ofFilteredPaths
     * @see JarContents#ofPaths
     */
    public CompositeJarContents(List<JarContents> delegates, @Nullable List<@Nullable PathFilter> filters) {
        // These checks are to prevent suboptimal usage. JarContents.ofFilteredPaths is going to guard against this
        if (delegates.isEmpty()) {
            throw new IllegalArgumentException("Cannot construct an empty mod container");
        }
        if (filters != null && delegates.size() != filters.size()) {
            throw new IllegalArgumentException("The number of delegates and filters must match.");
        }
        if (delegates.size() == 1 && (filters == null || filters.getFirst() == null)) {
            throw new IllegalArgumentException("Can only construct a composite jar contents with multiple delegates or at least one filter.");
        }

        // Internally the first match gets returned, but the user supplies the list of paths
        // assuming later entries override earlier entries, so we reverse.
        delegates = delegates.reversed();
        filters = filters != null ? filters.reversed() : null;

        this.delegates = delegates.toArray(JarContents[]::new);
        this.filters = filters != null ? filters.toArray(PathFilter[]::new) : null;

        // Collect all unique content roots of our delegates
        var contentRoots = new ArrayList<Path>();
        for (var delegate : delegates) {
            for (var root : delegate.getContentRoots()) {
                if (!contentRoots.contains(root)) {
                    contentRoots.add(root);
                }
            }
        }
        this.contentRoots = List.copyOf(contentRoots.reversed());
    }

    @Override
    public Collection<Path> getContentRoots() {
        return contentRoots;
    }

    public boolean isFiltered() {
        return filters != null && Arrays.stream(filters).anyMatch(Objects::nonNull);
    }

    /**
     * For a composite jar, we compute the checksum as the SHA-256 of the concatenation of the
     * checksums of the delegates, in order. If the composite jar is filtered, or any delegate
     * does not have a checksum, then the composite jar does not have a checksum.
     */
    @Override
    public Optional<String> getChecksum() {
        var checksum = this.checksum;
        if (checksum == null) {
            synchronized (this) {
                checksum = this.checksum;
                if (checksum == null) {
                    this.checksum = checksum = computeChecksum();
                }
            }
        }
        return checksum;
    }

    private Optional<String> computeChecksum() {
        if (isFiltered()) {
            return Optional.empty();
        }

        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        for (var delegate : delegates) {
            var delegateChecksum = delegate.getChecksum();
            if (delegateChecksum.isEmpty()) {
                return Optional.empty();
            }

            digest.update(delegateChecksum.get().getBytes());
        }

        byte[] checksum = digest.digest();
        return Optional.of(HexFormat.of().formatHex(checksum));
    }

    @Override
    public Path getPrimaryPath() {
        // The last delegate corresponds to the first entry given by the user (we reverse)
        return delegates[delegates.length - 1].getPrimaryPath();
    }

    @Override
    public String toString() {
        var result = new StringBuilder("composite(");
        // In reverse order as that is how the user passed it
        for (int i = delegates.length - 1; i >= 0; i--) {
            var delegate = delegates[i];
            if (filters != null && filters[i] != null) {
                result.append("filtered(").append(delegate).append(")");
            } else {
                result.append(delegate);
            }
            if (i != 0) {
                result.append(", ");
            }
        }
        result.append(")");
        return result.toString();
    }

    @Override
    public Optional<URI> findFile(String relativePath) {
        for (int i = 0; i < delegates.length; i++) {
            if (isMasked(i, relativePath)) {
                continue;
            }
            var delegate = delegates[i];
            var result = delegate.findFile(relativePath);
            if (result.isPresent()) {
                return result;
            }
        }
        return Optional.empty();
    }

    @Override
    public Manifest getManifest() {
        for (int i = 0; i < delegates.length; i++) {
            if (isMasked(i, JarFile.MANIFEST_NAME)) {
                continue;
            }
            var delegate = delegates[i];
            var manifest = delegate.getManifest();
            // Since we always return a non-null manifest, we consider empty manifests to be "missing"
            if (!manifest.getMainAttributes().isEmpty() || !manifest.getEntries().isEmpty()) {
                return manifest;
            }
        }
        return EmptyManifest.INSTANCE;
    }

    @Override
    public @Nullable JarResource get(String relativePath) {
        for (int i = 0; i < delegates.length; i++) {
            if (isMasked(i, relativePath)) {
                continue;
            }
            var delegate = delegates[i];
            var resource = delegate.get(relativePath);
            if (resource != null) {
                return resource;
            }
        }
        return null;
    }

    @Override
    public boolean containsFile(String relativePath) {
        for (int i = 0; i < delegates.length; i++) {
            if (isMasked(i, relativePath)) {
                continue;
            }
            var delegate = delegates[i];
            if (delegate.containsFile(relativePath)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public InputStream openFile(String relativePath) throws IOException {
        for (int i = 0; i < delegates.length; i++) {
            if (isMasked(i, relativePath)) {
                continue;
            }
            var delegate = delegates[i];
            var stream = delegate.openFile(relativePath);
            if (stream != null) {
                return stream;
            }
        }
        return null;
    }

    @Override
    public byte[] readFile(String relativePath) throws IOException {
        for (int i = 0; i < delegates.length; i++) {
            if (isMasked(i, relativePath)) {
                continue;
            }
            var delegate = delegates[i];
            var content = delegate.readFile(relativePath);
            if (content != null) {
                return content;
            }
        }
        return null;
    }

    @Override
    public void visitContent(String startingFolder, JarResourceVisitor visitor) {
        // This is based on the logic that openResource will return the file from the *first* delegate
        // Every relative path we return will not be returned again
        var distinctVisitor = new JarResourceVisitor() {
            final Set<String> pathsVisited = new HashSet<>();
            int delegateIdx;

            @Override
            public void visit(String relativePath, JarResource resource) {
                if (!isMasked(delegateIdx, relativePath) && pathsVisited.add(relativePath)) {
                    visitor.visit(relativePath, resource);
                }
            }
        };

        for (int i = 0; i < delegates.length; i++) {
            var delegate = delegates[i];
            distinctVisitor.delegateIdx = i;
            delegate.visitContent(startingFolder, distinctVisitor);
        }
    }

    @Override
    public void close() throws IOException {
        IOException error = null;
        for (var delegate : delegates) {
            try {
                delegate.close();
            } catch (IOException e) {
                if (error == null) {
                    error = new IOException("Failed to close one ore more delegates of " + this);
                }
                error.addSuppressed(e);
            }
        }
        if (error != null) {
            throw error;
        }
    }

    /**
     * {@return {@code true} if the given relative path is hidden by the given delegates filter}
     */
    private boolean isMasked(int delegateIdx, String relativePath) {
        if (filters == null) {
            return false;
        }
        var filter = filters[delegateIdx];
        return filter != null && !filter.test(relativePath);
    }

    public List<JarContents> getDelegates() {
        return List.of(delegates);
    }
}
