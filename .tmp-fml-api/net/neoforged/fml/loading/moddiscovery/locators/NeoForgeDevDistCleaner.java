/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.loading.moddiscovery.locators;

import com.mojang.logging.LogUtils;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModLoadingException;
import net.neoforged.fml.ModLoadingIssue;
import net.neoforged.fml.jarcontents.JarContents;
import net.neoforged.neoforgespi.transformation.ClassProcessor;
import net.neoforged.neoforgespi.transformation.ClassProcessorIds;
import net.neoforged.neoforgespi.transformation.ProcessorName;
import org.jetbrains.annotations.ApiStatus;
import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

/**
 * Masks classes from the Minecraft jar that are for the wrong distribution in a development environment, throwing an
 * informative exception.
 */
@ApiStatus.Internal
public class NeoForgeDevDistCleaner implements ClassProcessor {
    private static final Attributes.Name NAME_DISTS = new Attributes.Name("Minecraft-Dists");
    private static final Attributes.Name NAME_DIST = new Attributes.Name("Minecraft-Dist");
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Marker DISTXFORM = MarkerFactory.getMarker("DISTXFORM");

    private final Dist dist;
    private final Set<String> maskedClasses;

    public NeoForgeDevDistCleaner(JarContents minecraftModFile, Dist requestedDist) {
        this.dist = requestedDist;
        this.maskedClasses = getMaskedFiles(minecraftModFile, requestedDist)
                .map(path -> {
                    // Classes are kept, but set to be filtered out at runtime; resources are removed entirely.
                    if (path.endsWith(".class")) {
                        return path.substring(0, path.length() - ".class".length()).replace('/', '.');
                    } else {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    public static boolean supportsDistCleaning(JarContents minecraftModFile) {
        return minecraftModFile.getManifest().getMainAttributes().containsKey(NAME_DISTS);
    }

    @Override
    public ProcessorName name() {
        return ClassProcessorIds.DIST_CLEANER;
    }

    @Override
    public Set<ProcessorName> runsBefore() {
        // Might as well run as early as we sensibly can, so that we can catch issues before other transformers run their checks
        return Set.of(ClassProcessorIds.COMPUTING_FRAMES);
    }

    @Override
    public Set<ProcessorName> runsAfter() {
        return Set.of();
    }

    @Override
    public ComputeFlags processClass(TransformationContext context) {
        return ComputeFlags.NO_REWRITE;
    }

    @Override
    public boolean handlesClass(SelectionContext context) {
        if (maskedClasses.contains(context.type().getClassName())) {
            String message = String.format("Attempted to load class %s which is not present on the %s", context.type().getClassName(), switch (dist) {
                case CLIENT -> "client";
                case DEDICATED_SERVER -> "dedicated server";
            });
            LOGGER.error(DISTXFORM, message);
            // We must sneakily throw a (usually checked) ClassNotFoundException here. This is necessary so that java's
            // initialization logic produces a NoClassDefFoundError in dev, like it would in prod, when a class
            // referencing such a class is loaded. Though this should not often matter, as errors cannot generally be
            // caught in a recoverable fashion, they may still be caught for debugging purposes or the like so it is
            // best to be consistent here.
            throwUnchecked(new ClassNotFoundException(message));
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private static <T, X extends Throwable> void throwUnchecked(T throwable) throws X {
        throw (X) throwable;
    }

    /**
     * Loads file masking information from the jar's manifest, masking resource files that should not be present and
     * telling {@link NeoForgeDevDistCleaner} to clean class files that should be masked.
     */
    public static Stream<String> getMaskedFiles(JarContents minecraftJar, Dist currentDist) {
        var manifest = minecraftJar.getManifest();
        String dists = manifest.getMainAttributes().getValue(NAME_DISTS);
        if (dists == null) {
            // Jar has no masking attributes; in dev, this is necessary
            throw new ModLoadingException(ModLoadingIssue.error("fml.modloadingissue.neodev_missing_dists_attribute", NAME_DISTS));
        }
        var dist = switch (currentDist) {
            case CLIENT -> "client";
            case DEDICATED_SERVER -> "server";
        };
        if (Arrays.stream(dists.split("\\s+")).allMatch(s -> s.equals(dist))) {
            return Stream.empty();
        }
        if (Arrays.stream(dists.split("\\s+")).noneMatch(s -> s.equals(dist))) {
            // Jar has no marker for the current dist; this is wacky and should not occur
            throw new ModLoadingException(ModLoadingIssue.error("fml.modloadingissue.neodev_missing_appropriate_dist", dist, NAME_DISTS));
        }

        return manifest.getEntries().entrySet().stream()
                .filter(entry -> {
                    var fileDist = entry.getValue().getValue(NAME_DIST);
                    return fileDist != null && !fileDist.equals(dist);
                })
                .map(Map.Entry::getKey);
    }
}
