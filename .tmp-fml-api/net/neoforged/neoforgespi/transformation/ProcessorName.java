/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforgespi.transformation;

import java.util.Comparator;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * A name for a processor; enforces the same rules as a Minecraft ResourceLocation
 */
public record ProcessorName(String namespace, String path) implements Comparable<ProcessorName> {
    private static final Pattern NAMESPACE_PATTERN = Pattern.compile("^[a-z0-9_.-]+$");
    private static final Pattern PATH_PATTERN = Pattern.compile("^[a-z0-9_./-]+$");

    public ProcessorName {
        Objects.requireNonNull(namespace, "namespace");
        Objects.requireNonNull(path, "path");
        // We enforce the same requirements as ResourceLocation
        if (!NAMESPACE_PATTERN.asMatchPredicate().test(namespace)) {
            throw new IllegalArgumentException("Invalid namespace for processor name: " + namespace);
        }
        if (!PATH_PATTERN.asMatchPredicate().test(path)) {
            throw new IllegalArgumentException("Invalid path for processor name: " + path);
        }
    }

    @Override
    public String toString() {
        return namespace + ":" + path;
    }

    public static ProcessorName parse(String fullName) {
        var parts = fullName.split(":", 2);
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid processor name: " + fullName);
        }
        return new ProcessorName(parts[0], parts[1]);
    }

    @Override
    public int compareTo(ProcessorName other) {
        return Comparator.comparing(ProcessorName::namespace)
                .thenComparing(ProcessorName::path)
                .compare(this, other);
    }
}
