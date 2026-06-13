/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.classloading.transformation;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import net.neoforged.neoforgespi.transformation.ClassProcessor;
import net.neoforged.neoforgespi.transformation.ClassProcessorIds;
import net.neoforged.neoforgespi.transformation.ProcessorName;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public class ClassTransformStatistics {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final Map<ProcessorName, Integer> TRANSFORMS_BY_PROCESSOR = new ConcurrentHashMap<>();
    private static final Map<ProcessorName, Integer> POTENTIAL_BY_PROCESSOR = new ConcurrentHashMap<>();
    private static final AtomicInteger LOADED_CLASS_COUNT = new AtomicInteger(0);
    private static final AtomicInteger TRANSFORMED_CLASS_COUNT = new AtomicInteger(0);
    private static final AtomicInteger MIXIN_PARSED_CLASS_COUNT = new AtomicInteger(0);

    public static void incrementMixinParsedClasses() {
        MIXIN_PARSED_CLASS_COUNT.incrementAndGet();
    }

    static void incrementAskedForTransform(ClassProcessor processor) {
        if (!processor.name().equals(ClassProcessorIds.COMPUTING_FRAMES)) {
            POTENTIAL_BY_PROCESSOR.compute(processor.name(), (k, v) -> v == null ? 1 : v + 1);
        }
    }

    static void incrementTransforms(ClassProcessor processor) {
        if (!processor.name().equals(ClassProcessorIds.COMPUTING_FRAMES)) {
            TRANSFORMS_BY_PROCESSOR.compute(processor.name(), (k, v) -> v == null ? 1 : v + 1);
        }
    }

    static void incrementLoadedClasses() {
        LOADED_CLASS_COUNT.incrementAndGet();
    }

    static void incrementTransformedClasses() {
        TRANSFORMED_CLASS_COUNT.incrementAndGet();
    }

    @ApiStatus.Internal
    public static synchronized String getTransformationSummary() {
        var loaded = LOADED_CLASS_COUNT.get();
        var transformed = TRANSFORMED_CLASS_COUNT.get();
        double ratio = loaded == 0 ? 0d : ((double) transformed) / loaded * 100;
        return String.format("%s/%s (%.2f%%)", transformed, loaded, ratio);
    }

    @ApiStatus.Internal
    public static String getMixinParsedClassesSummary() {
        return String.valueOf(MIXIN_PARSED_CLASS_COUNT.get());
    }

    @ApiStatus.Internal
    public static synchronized void logTransformationSummary() {
        LOGGER.debug("Transformed/total loaded classes: {} and {} parsed for mixin", getTransformationSummary(), getMixinParsedClassesSummary());
    }

    @ApiStatus.Internal
    public synchronized static void checkTransformationBehavior() {
        // Checks if any transformers are acting suspiciously like they're targeting everything; logs an error for any
        // that are.
        var keys = new ArrayList<>(TRANSFORMS_BY_PROCESSOR.keySet());
        keys.forEach(name -> {
            var actual = TRANSFORMS_BY_PROCESSOR.get(name);
            var potential = POTENTIAL_BY_PROCESSOR.get(name);
            var ratio = ((double) actual) / potential;
            if (ratio > 0.25) {
                if (name.equals(ClassProcessorIds.MIXIN)) {
                    // We special-case mixin in order to provide a more useful message, as the root issue here could be
                    // a bad coprocessor. 
                    LOGGER.error("Class processor {} transformed {}% of loaded class which is suspiciously high; this could be due to a mixin coprocessor attempting mass-ASM", name, String.format("%.2f", ratio * 100));
                } else {
                    LOGGER.error("Class processor {} transformed {}% of loaded class which is suspiciously high; it may be attempting mass-ASM. Please report this to the mod author.", name, String.format("%.2f", ratio * 100));
                }
            }
        });
    }

    @ApiStatus.Internal
    public static synchronized String computeCrashReportEntry(ClassProcessorSet classProcessorSet) {
        var transforms = classProcessorSet.getSortedProcessors();
        record Entry(double ratio, String name) {}
        var entries = new ArrayList<Entry>();
        for (var transform : transforms) {
            var name = transform.name();
            var actual = TRANSFORMS_BY_PROCESSOR.get(name);
            var potential = POTENTIAL_BY_PROCESSOR.get(name);
            double ratio;
            if (actual == null || potential == null) {
                ratio = 0;
            } else {
                ratio = 100d * ((double) actual) / potential;
            }
            entries.add(new Entry(ratio, classProcessorSet.isMarker(transform) ? name + " (marker)" : name.toString()));
        }
        return entries.stream()
                .map(e -> String.format("%05.2f%%: %s", e.ratio, e.name))
                .collect(Collectors.joining("\n\t\t", "\n\t\t", ""));
    }
}
