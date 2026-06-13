/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.util;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import net.neoforged.fml.ModLoadingException;
import net.neoforged.fml.ModLoadingIssue;
import net.neoforged.fml.loading.LogMarkers;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApiStatus.Internal
public final class ClasspathResourceUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClasspathResourceUtils.class);

    private ClasspathResourceUtils() {}

    public static List<Path> findFileSystemRootsOfFileOnClasspath(String relativePath) {
        // If we're loaded through a module, it means the original classpath is inaccessible through the context CL
        var classLoader = Thread.currentThread().getContextClassLoader();
        if (ClasspathResourceUtils.class.getModule().isNamed()) {
            classLoader = ClassLoader.getSystemClassLoader();
        }

        return findFileSystemRootsOfFileOnClasspath(classLoader, relativePath);
    }

    public static Path findJarPathFor(String resourceName, String jarName, URL resource) {
        try {
            Path path;
            URI uri = resource.toURI();
            if (uri.getScheme().equals("jar") && uri.getRawSchemeSpecificPart().contains("!/")) {
                int lastExcl = uri.getRawSchemeSpecificPart().lastIndexOf("!/");
                path = Paths.get(new URI(uri.getRawSchemeSpecificPart().substring(0, lastExcl)));
            } else {
                path = Paths.get(new URI("file://" + uri.getRawSchemeSpecificPart().substring(0, uri.getRawSchemeSpecificPart().length() - resourceName.length())));
            }
            //LOGGER.debug(CORE, "Found JAR {} at path {}", jarName, path.toString());
            return path;
        } catch (NullPointerException | URISyntaxException e) {
            LOGGER.error(LogMarkers.SCAN, "Failed to find JAR for class {} - {}", resourceName, jarName);
            throw new RuntimeException("Unable to locate " + resourceName + " - " + jarName, e);
        }
    }

    public static Path getRootFromResourceUrl(String relativePath, URL resourceUrl) {
        if ("jar".equals(resourceUrl.getProtocol())) {
            var fileUri = URI.create(resourceUrl.toString().split("!")[0].substring("jar:".length()));
            return Paths.get(fileUri);
        } else {
            Path resourcePath;
            try {
                resourcePath = Paths.get(resourceUrl.toURI());
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException("Failed to convert " + resourceUrl + " to URI");
            }

            // Walk back from the resource path up to the content root that contained it
            var current = Paths.get(relativePath);
            while (current != null) {
                current = current.getParent();
                resourcePath = resourcePath.getParent();
                if (resourcePath == null) {
                    throw new IllegalArgumentException("Resource " + resourceUrl + " did not have same nesting depth as " + relativePath);
                }
            }

            return resourcePath;
        }
    }

    @Nullable
    public static Path findFileSystemRootOfFileOnClasspath(ClassLoader classLoader, String relativePath) {
        var resource = classLoader.getResource(relativePath);
        if (resource == null) {
            return null;
        }

        return getRootFromResourceUrl(relativePath, resource);
    }

    public static List<Path> findFileSystemRootsOfFileOnClasspath(ClassLoader classLoader, String relativePath) {
        // Find the directory that contains the Minecraft classes via the system classpath
        Iterator<URL> resourceIt;
        try {
            resourceIt = classLoader.getResources(relativePath).asIterator();
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to enumerate classpath locations of " + relativePath);
        }

        var result = new LinkedHashSet<Path>();
        while (resourceIt.hasNext()) {
            var resourceUrl = resourceIt.next();
            result.add(getRootFromResourceUrl(relativePath, resourceUrl));
        }

        return new ArrayList<>(result);
    }

    public static Path findFileSystemRootOfFileOnClasspath(String relativePath) {
        var paths = findFileSystemRootsOfFileOnClasspath(relativePath);

        if (paths.isEmpty()) {
            throw new ModLoadingException(ModLoadingIssue.error("fml.modloadingissue.failed_to_find_on_classpath", relativePath));
        } else if (paths.size() > 1) {
            throw new ModLoadingException(ModLoadingIssue.error("fml.modloadingissue.multiple_copies_on_classpath", relativePath, paths));
        }

        return paths.getFirst();
    }

    public static Set<Path> getAllClasspathItems(ClassLoader loader) {
        var result = new HashSet<Path>();
        if (loader == ClassLoader.getSystemClassLoader()) {
            return Arrays.stream(System.getProperty("java.class.path").split(File.pathSeparator))
                    .map(Paths::get)
                    .collect(Collectors.toSet());
        } else if (loader instanceof URLClassLoader urlClassLoader) {
            for (var url : urlClassLoader.getURLs()) {
                if ("file".equals(url.getProtocol())) {
                    try {
                        result.add(Paths.get(url.toURI()));
                    } catch (URISyntaxException ignored) {}
                }
            }
        }

        if (loader.getParent() != null && loader.getParent() != ClassLoader.getPlatformClassLoader()) {
            result.addAll(getAllClasspathItems(loader.getParent()));
        }

        return result;
    }
}
