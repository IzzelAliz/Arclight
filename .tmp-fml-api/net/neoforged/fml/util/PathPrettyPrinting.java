/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.util;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Helper for pretty-printing paths for end-users.
 * Aims to shorten paths by substituting well-known locations and relativizing to the game directory, for example.
 */
public final class PathPrettyPrinting {
    /**
     * Try long prefixes before short prefixes.
     */
    private static final Comparator<PathSubstitution> SUBSTITUTION_COMPARATOR = Comparator.comparingInt((PathSubstitution alias) -> alias.basePath.getNameCount()).reversed();

    private static volatile List<PathSubstitution> SUBSTITUTIONS = new ArrayList<>();

    private PathPrettyPrinting() {}

    public static void addRoot(Path root) {
        addSubstitution(root, "", "");
    }

    public static void addSubstitution(Path root, String prefix, String suffix) {
        var newSubstitutions = new ArrayList<>(SUBSTITUTIONS);
        newSubstitutions.add(new PathSubstitution(root, prefix, suffix));
        newSubstitutions.sort(SUBSTITUTION_COMPARATOR);
        SUBSTITUTIONS = newSubstitutions;
    }

    public static String prettyPrint(Path path) {
        String resultPath = null;

        var currentSubstitutions = SUBSTITUTIONS;
        for (var substitution : currentSubstitutions) {
            if (path.startsWith(substitution.basePath)) {
                resultPath = substitution.prefix + substitution.basePath.relativize(path)
                        + substitution.suffix;
                break;
            }
        }

        // No known prefix, it might come from Gradle like in dev, or similar
        if (resultPath == null) {
            if (Files.isDirectory(path)) {
                resultPath = path.toAbsolutePath().toString();
            } else {
                resultPath = path.toString();
            }
        }

        // Unify separators to ensure it is easier to test
        resultPath = resultPath.replace('\\', '/');

        return resultPath;
    }

    private record PathSubstitution(Path basePath, String prefix, String suffix) {}
}
