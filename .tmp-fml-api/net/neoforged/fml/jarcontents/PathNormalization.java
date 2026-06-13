/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.jarcontents;

final class PathNormalization {
    private static final char SEPARATOR = '/';

    private PathNormalization() {}

    public static void assertNormalized(CharSequence path) {
        if (!isNormalized(path)) {
            throw new IllegalArgumentException("Path is not a valid relative path: " + path);
        }
    }

    public static boolean isNormalized(CharSequence path) {
        if (path.isEmpty()) {
            return true; // This will fail for other reasons
        }

        // Normalized paths use forward slashes
        for (int i = 0; i < path.length(); i++) {
            if (path.charAt(i) == '\\') {
                return false;
            }
        }

        char prevCh = '\0';
        int segmentStart = 0;
        for (int i = 0; i < path.length(); i++) {
            boolean atEnd = i == path.length() - 1;

            char ch = path.charAt(i);
            if ((i == 0 || atEnd) && ch == SEPARATOR) {
                return false; // No leading or trailing separators
            }
            if (ch == SEPARATOR && prevCh == SEPARATOR) {
                return false; // No repeated separators
            }
            // Validate path segments either when encounter separators or at the last character
            if (ch == SEPARATOR || atEnd) {
                var segmentEnd = ch == SEPARATOR ? i - 1 : i;
                var segmentLength = (segmentEnd - segmentStart) + 1;
                if (segmentLength == 1 && path.charAt(segmentStart) == '.') {
                    return false; // No '.' segments
                } else if (segmentLength == 2 && path.charAt(segmentStart) == '.' && path.charAt(segmentEnd) == '.') {
                    return false; // No '..' segments
                }

                segmentStart = i + 1;
            }
            prevCh = ch;
        }

        return true;
    }

    public static String normalize(CharSequence path) {
        return normalize(path, false);
    }

    /**
     * Normalizing a folder prefix ensures that non-empty paths end with a separator.
     */
    public static String normalizeFolderPrefix(CharSequence path) {
        return normalize(path, true);
    }

    private static String normalize(CharSequence path, boolean folderPrefix) {
        var result = new StringBuilder(path.length());

        int startOfSegment = 0;
        for (int i = 0; i < path.length(); i++) {
            char ch = path.charAt(i);
            if (ch == '\\') {
                ch = SEPARATOR;
            }
            if (ch == SEPARATOR) {
                if (i > startOfSegment) {
                    if (!result.isEmpty()) {
                        result.append(SEPARATOR);
                    }

                    var segment = path.subSequence(startOfSegment, i);
                    validateSegment(segment);
                    result.append(segment);
                }
                startOfSegment = i + 1;
            }
        }
        if (startOfSegment < path.length()) {
            if (!result.isEmpty()) {
                result.append(SEPARATOR);
            }
            var segment = path.subSequence(startOfSegment, path.length());
            validateSegment(segment);
            result.append(segment);
        }

        if (folderPrefix && !result.isEmpty()) {
            result.append(SEPARATOR);
        }

        return result.toString();
    }

    private static void validateSegment(CharSequence segment) {
        if (segment.equals(".") || segment.equals("..")) {
            throw new IllegalArgumentException("./ or ../ segments in paths are not supported");
        }
    }
}
