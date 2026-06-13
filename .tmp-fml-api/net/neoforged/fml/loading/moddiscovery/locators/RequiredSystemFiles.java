/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.loading.moddiscovery.locators;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.jar.Manifest;
import java.util.stream.Stream;
import net.neoforged.fml.ModLoadingException;
import net.neoforged.fml.ModLoadingIssue;
import net.neoforged.fml.jarcontents.JarContents;
import net.neoforged.fml.loading.moddiscovery.readers.JarModsDotTomlModFileReader;
import net.neoforged.fml.util.ClasspathResourceUtils;
import net.neoforged.neoforgespi.ILaunchContext;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Models the required files for opening a valid Minecraft and NeoForge jar from the classpath,
 * and supports various deployment models.
 */
final class RequiredSystemFiles implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(RequiredSystemFiles.class);

    static final String COMMON_CLASS = "net/minecraft/DetectedVersion.class";
    static final String CLIENT_CLASS = "net/minecraft/client/Minecraft.class";
    static final String COMMON_RESOURCE_ROOT = "data/.mcassetsroot";
    static final String CLIENT_RESOURCE_ROOT = "assets/.mcassetsroot";
    static final String NEOFORGE_COMMON_CLASS = "net/neoforged/neoforge/common/NeoForgeMod.class";
    static final String NEOFORGE_CLIENT_CLASS = "net/neoforged/neoforge/client/ClientNeoForgeMod.class";
    private JarContents commonClasses;
    private JarContents commonResources;
    private JarContents clientClasses;
    private JarContents clientResources;
    private JarContents neoForgeCommonClasses;
    private JarContents neoForgeClientClasses;
    private JarContents neoForgeResources;

    private RequiredSystemFiles() {}

    public boolean areNeoForgeAndMinecraftSeparate() {
        // Any intersection between Minecraft and NeoForge components means we have to filter jars
        return Collections.disjoint(getMinecraftJarComponents(), getNeoForgeJarComponents());
    }

    public void checkForMissingMinecraftFiles(boolean clientRequired) {
        var missingFiles = new ArrayList<String>();

        if (commonClasses == null) {
            missingFiles.add(COMMON_CLASS);
        }
        if (commonResources == null) {
            missingFiles.add(COMMON_RESOURCE_ROOT);
        }
        if (neoForgeCommonClasses == null) {
            missingFiles.add(NEOFORGE_COMMON_CLASS);
        }
        if (neoForgeResources == null) {
            missingFiles.add("NeoForge MANIFEST.MF");
        }

        if (clientRequired) {
            if (clientClasses == null) {
                missingFiles.add(CLIENT_CLASS);
            }
            if (clientResources == null) {
                missingFiles.add(CLIENT_RESOURCE_ROOT);
            }
            if (neoForgeClientClasses == null) {
                missingFiles.add(NEOFORGE_CLIENT_CLASS);
            }
        }

        // Note that finding a common class is the minimum requirement, since other classes are available from the obfuscated client jar
        if (!missingFiles.isEmpty()) {
            var foundRoots = getAll().stream().distinct().toList();
            LOG.error("Couldn't find {} on classpath, while we did find other required files in: {}", missingFiles, foundRoots);
            throw new ModLoadingException(ModLoadingIssue.error("fml.modloadingissue.missing_minecraft_jar"));
        }
    }

    public static RequiredSystemFiles find(ILaunchContext context, ClassLoader loader) {
        var locatedRoots = new ArrayList<JarContents>();

        var result = new RequiredSystemFiles();
        try {
            result.commonClasses = findAndOpen(context, loader, locatedRoots, COMMON_CLASS);
            result.commonResources = findAndOpen(context, loader, locatedRoots, COMMON_RESOURCE_ROOT);
            result.clientClasses = findAndOpen(context, loader, locatedRoots, CLIENT_CLASS);
            result.clientResources = findAndOpen(context, loader, locatedRoots, CLIENT_RESOURCE_ROOT);
            result.neoForgeCommonClasses = findAndOpen(context, loader, locatedRoots, NEOFORGE_COMMON_CLASS);
            result.neoForgeClientClasses = findAndOpen(context, loader, locatedRoots, NEOFORGE_CLIENT_CLASS);
            result.neoForgeResources = findNeoForgeResources(locatedRoots, loader);
        } catch (Exception e) {
            closeAll(locatedRoots);
            throw e;
        }
        return result;
    }

    private static JarContents findNeoForgeResources(List<JarContents> locatedRoots, ClassLoader loader) {
        // Check if any of the opened jar files already pointed to the Manifest.
        // If the NeoForge classes are already packaged into a jar, it will contain the manifest too.
        for (var root : locatedRoots) {
            if (isNeoForgeManifest(root.getManifest())) {
                LOG.debug("Found NeoForge MANIFEST.MF in {}", root);
                return root;
            }
        }

        // We look for all MANIFEST.MF directly on the classpath and try to find the one for NeoForge
        var manifestRoots = ClasspathResourceUtils.findFileSystemRootsOfFileOnClasspath(loader, JarModsDotTomlModFileReader.MANIFEST);
        for (var manifestRoot : manifestRoots) {
            if (!Files.isDirectory(manifestRoot)) {
                // We only scan directories here, since if it was part of a Jar, it'd also have classes, which we
                // would have already found (see the loop above).
                continue;
            }

            if (isNeoForgeManifest(manifestRoot.resolve(JarModsDotTomlModFileReader.MANIFEST))) {
                return openOrThrow(manifestRoot);
            }
        }

        return null;
    }

    @Nullable
    private static JarContents findAndOpen(ILaunchContext context,
            ClassLoader loader,
            List<JarContents> alreadyOpened,
            String relativePath) {
        // First test the already opened jars for speed
        for (var contents : alreadyOpened) {
            if (contents.containsFile(relativePath)) {
                return contents;
            }
        }

        var roots = ClasspathResourceUtils.findFileSystemRootsOfFileOnClasspath(loader, relativePath);

        for (var path : roots) {
            // The obfuscated client jar is on the classpath in production, and we mark it as located earlier.
            // This check prevents us trying to load our files from it.
            if (!context.isLocated(path)) {
                var jar = openOrThrow(path);
                alreadyOpened.add(jar);
                return jar;
            }
        }

        return null;
    }

    private static JarContents openOrThrow(Path root) {
        try {
            return JarContents.ofPath(root);
        } catch (IOException e) {
            throw new ModLoadingException(ModLoadingIssue.error("fml.modloadingissue.corrupted_minecraft_jar").withAffectedPath(root).withCause(e));
        }
    }

    private static boolean isNeoForgeManifest(Path path) {
        // TODO: We should use some other build-time-only approach of marking the directories, same as we do for userdev
        try (var in = new BufferedInputStream(Files.newInputStream(path))) {
            var manifest = new Manifest(in);
            return isNeoForgeManifest(manifest);
        } catch (IOException e) {
            LOG.debug("Failed to read manifest at {}: {}", path, e);
            return false;
        }
    }

    private static boolean isNeoForgeManifest(Manifest manifest) {
        return "neoforge".equals(manifest.getMainAttributes().getValue("FML-System-Mods"));
    }

    public List<JarContents> getAll() {
        return Stream.of(commonClasses,
                commonResources,
                clientClasses,
                clientResources,
                neoForgeCommonClasses,
                neoForgeClientClasses,
                neoForgeResources)
                .filter(Objects::nonNull)
                .toList();
    }

    public List<JarContents> getClassesRoots() {
        return Stream.of(commonClasses,
                clientClasses,
                neoForgeCommonClasses,
                neoForgeClientClasses)
                .filter(Objects::nonNull)
                .toList();
    }

    public boolean isEmpty() {
        return getAll().isEmpty();
    }

    public JarContents getCommonClasses() {
        return commonClasses;
    }

    public JarContents getCommonResources() {
        return commonResources;
    }

    public JarContents getClientClasses() {
        return clientClasses;
    }

    public JarContents getClientResources() {
        return clientResources;
    }

    public JarContents getNeoForgeCommonClasses() {
        return neoForgeCommonClasses;
    }

    public JarContents getNeoForgeClientClasses() {
        return neoForgeClientClasses;
    }

    public JarContents getNeoForgeResources() {
        return neoForgeResources;
    }

    private static void closeAll(Iterable<JarContents> locatedRoots) {
        for (var root : locatedRoots) {
            try {
                root.close();
            } catch (IOException ex) {
                LOG.error("Couldn't close Minecraft jar file.", ex);
            }
        }
    }

    @Override
    public void close() {
        closeAll(getAll());
    }

    public List<JarContents> getMinecraftJarComponents() {
        return uniqueAndNonNull(commonResources, commonClasses, clientResources, clientClasses);
    }

    public List<JarContents> getNeoForgeJarComponents() {
        return uniqueAndNonNull(neoForgeResources, neoForgeCommonClasses, neoForgeClientClasses);
    }

    private static List<JarContents> uniqueAndNonNull(JarContents... contents) {
        var result = new ArrayList<JarContents>(contents.length);
        for (var content : contents) {
            if (content != null && !result.contains(content)) {
                result.add(content);
            }
        }
        return result;
    }
}
