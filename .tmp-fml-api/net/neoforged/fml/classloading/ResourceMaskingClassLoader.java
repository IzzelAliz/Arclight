/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.classloading;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import net.neoforged.fml.util.ClasspathResourceUtils;
import org.jetbrains.annotations.Nullable;

/**
 * This class loader will filter the resources returned from {@link #getResources(String)} and {@link #getResource(String)}
 * by removing any results that come from a given set of classpath items (folders or jars).
 */
public class ResourceMaskingClassLoader extends ClassLoader {
    static {
        ClassLoader.registerAsParallelCapable();
    }

    private final Set<Path> maskedClasspathElements;

    public ResourceMaskingClassLoader(ClassLoader parent, Set<Path> maskedClasspathElements) {
        super(Objects.requireNonNull(parent, "parent"));
        this.maskedClasspathElements = maskedClasspathElements;
    }

    @Override
    public @Nullable URL getResource(String name) {
        // This is a very tricky thing: if the resource points to one of the filtered paths,
        // We need to use getResources() and find the next resource *not* matching the filter.
        var resource = getParent().getResource(name);
        if (resource == null) {
            return null;
        }

        var resourceRoot = ClasspathResourceUtils.getRootFromResourceUrl(name, resource);
        if (maskedClasspathElements.contains(resourceRoot)) {
            try {
                var resources = getResources(name);
                resource = resources.hasMoreElements() ? resources.nextElement() : null;
            } catch (IOException e) {
                resource = null;
            }
        }

        return resource;
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        return new FilteringEnumeration(super.getResources(name), name);
    }

    /**
     * Filters an enumeration of URLs by using the given predicate.
     */
    class FilteringEnumeration implements Enumeration<URL> {
        private final Enumeration<URL> delegate;
        // Relative path that resources were requested for.
        private final String relativePath;

        @Nullable
        private URL nextElement;

        public FilteringEnumeration(Enumeration<URL> delegate, String relativePath) {
            this.delegate = delegate;
            this.relativePath = relativePath;
            seekNextElement();
        }

        @Override
        public boolean hasMoreElements() {
            return nextElement != null;
        }

        @Override
        public URL nextElement() {
            var result = nextElement;
            if (result == null) {
                throw new NoSuchElementException();
            }
            seekNextElement();
            return result;
        }

        // Find the next element not within a masked classpath element
        private void seekNextElement() {
            while (delegate.hasMoreElements()) {
                var el = delegate.nextElement();
                Path root = ClasspathResourceUtils.getRootFromResourceUrl(relativePath, el);
                if (!maskedClasspathElements.contains(root)) {
                    nextElement = el;
                    return;
                }
            }
            nextElement = null; // No more elements
        }
    }
}
