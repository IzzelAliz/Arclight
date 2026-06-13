/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.jarmoduleinfo;

import java.util.Set;

final class JlsConstants {
    // https://docs.oracle.com/javase/specs/jls/se22/html/jls-3.html#jls-3.9
    static final Set<String> RESERVED_KEYWORDS = Set.of(
            "abstract",
            "assert",
            "boolean",
            "break",
            "byte",
            "case",
            "catch",
            "char",
            "class",
            "const",
            "continue",
            "default",
            "do",
            "double",
            "else",
            "enum",
            "extends",
            "final",
            "finally",
            "float",
            "for",
            "goto",
            "if",
            "implements",
            "import",
            "instanceof",
            "int",
            "interface",
            "long",
            "native",
            "new",
            "package",
            "private",
            "protected",
            "public",
            "return",
            "short",
            "static",
            "strictfp",
            "super",
            "switch",
            "synchronized",
            "this",
            "throw",
            "throws",
            "transient",
            "try",
            "void",
            "volatile",
            "while",
            // Not really keywords, but "boolean literals"
            "true",
            "false",
            // Not really a keyword, but the "null literal"
            "null",
            "_");

    // Same as jdk.internal.module.Checks.isJavaIdentifier
    public static boolean isJavaIdentifier(String str) {
        if (str.isEmpty() || RESERVED_KEYWORDS.contains(str)) {
            return false;
        }

        // This iterates over the Unicode code points instead of UTF-16 characters
        for (var i = 0; i < str.length();) {
            int codePoint = Character.codePointAt(str, i);

            if (i == 0 && !Character.isJavaIdentifierStart(codePoint)
                    || !Character.isJavaIdentifierPart(codePoint)) {
                return false;
            }

            i += Character.charCount(codePoint);
        }

        return true;
    }

    public static String getPackageName(String typeName) {
        var lastSeparator = typeName.lastIndexOf('.');
        if (lastSeparator != -1) {
            return typeName.substring(0, lastSeparator);
        }
        return "";
    }

    /**
     * Same as jdk.internal.module.Checks.isTypeName
     * <p>
     * Checks that every segment of the Java name (assuming period separators) is itself a valid Java identifier.
     */
    public static boolean isTypeName(String name) {
        int nextSep = -1;
        int lastSep = 0;
        // Iterate all segments
        for (nextSep = name.indexOf('.'); nextSep != -1; nextSep = name.indexOf('.', lastSep)) {
            var segment = name.substring(lastSep, nextSep);
            lastSep = nextSep + 1;

            if (!isJavaIdentifier(segment)) {
                return false;
            }
        }

        var lastSegment = name.substring(lastSep);
        return isJavaIdentifier(lastSegment);
    }

    private JlsConstants() {}
}
