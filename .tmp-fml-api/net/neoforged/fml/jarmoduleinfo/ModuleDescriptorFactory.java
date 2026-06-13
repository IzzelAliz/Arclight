/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.jarmoduleinfo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.module.InvalidModuleDescriptorException;
import java.lang.module.ModuleDescriptor;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import net.neoforged.fml.jarcontents.JarContents;
import net.neoforged.fml.jarcontents.JarResource;

/**
 * Utilities for creating {@link java.lang.module.ModuleDescriptor} from {@link JarContents}.
 * <p>
 * Most of this code also lives in the JDK in the internal ModulePath class.
 */
final class ModuleDescriptorFactory {
    private ModuleDescriptorFactory() {}

    // ALL from jdk.internal.module.ModulePath.java
    private static final Pattern DASH_VERSION = Pattern.compile("-([.\\d]+)");
    private static final Pattern NON_ALPHANUM = Pattern.compile("[^A-Za-z0-9]");
    private static final Pattern REPEATING_DOTS = Pattern.compile("(\\.)(\\1)+");
    private static final Pattern LEADING_DOTS = Pattern.compile("^\\.");
    private static final Pattern TRAILING_DOTS = Pattern.compile("\\.$");
    // Extra sanitization
    private static final Pattern MODULE_VERSION = Pattern.compile("(?<=^|-)([\\d][.\\d]*)");
    private static final Pattern NUMBERLIKE_PARTS = Pattern.compile("(?<=^|\\.)([0-9]+)"); // matches asdf.1.2b because both are invalid java identifiers
    private static final List<String> ILLEGAL_KEYWORDS = List.of(
            "abstract", "continue", "for", "new", "switch", "assert",
            "default", "goto", "package", "synchronized", "boolean",
            "do", "if", "private", "this", "break", "double", "implements",
            "protected", "throw", "byte", "else", "import", "public", "throws",
            "case", "enum", "instanceof", "return", "transient", "catch",
            "extends", "int", "short", "try", "char", "final", "interface",
            "static", "void", "class", "finally", "long", "strictfp",
            "volatile", "const", "float", "native", "super", "while");
    private static final Pattern KEYWORD_PARTS = Pattern.compile("(?<=^|\\.)(" + String.join("|", ILLEGAL_KEYWORDS) + ")(?=\\.|$)");

    static NameAndVersion computeNameAndVersion(Path path) {
        // detect Maven-like paths
        Path versionMaybe = path.getParent();
        if (versionMaybe != null) {
            Path artifactMaybe = versionMaybe.getParent();
            if (artifactMaybe != null) {
                Path artifactNameMaybe = artifactMaybe.getFileName();
                if (artifactNameMaybe != null && path.getFileName().toString().startsWith(artifactNameMaybe + "-" + versionMaybe.getFileName().toString())) {
                    var name = artifactMaybe.getFileName().toString();
                    var ver = versionMaybe.getFileName().toString();
                    var mat = MODULE_VERSION.matcher(ver);
                    if (mat.find()) {
                        var potential = ver.substring(mat.start());
                        ver = safeParseVersion(potential, path.getFileName().toString());
                        return new NameAndVersion(cleanModuleName(name), ver);
                    } else {
                        return new NameAndVersion(cleanModuleName(name), null);
                    }
                }
            }
        }

        // fallback parsing
        var fn = path.getFileName().toString();
        var lastDot = fn.lastIndexOf('.');
        if (lastDot > 0) {
            fn = fn.substring(0, lastDot); // strip extension if possible
        }

        var mat = DASH_VERSION.matcher(fn);
        if (mat.find()) {
            var potential = fn.substring(mat.start() + 1);
            var ver = safeParseVersion(potential, path.getFileName().toString());
            var name = mat.replaceAll("");
            return new NameAndVersion(cleanModuleName(name), ver);
        } else {
            return new NameAndVersion(cleanModuleName(fn), null);
        }
    }

    private static String safeParseVersion(String ver, String filename) {
        try {
            var len = ver.length();
            if (len == 0)
                throw new IllegalArgumentException("Error parsing version info from " + filename + ": Empty Version String");

            var last = ver.charAt(len - 1);
            if (last == '.' || last == '+' || last == '-') { //Attempt to filter out the common wrong file names.
                if (len == 1)
                    throw new IllegalArgumentException("Error parsing version info from " + filename + ": Invalid version \"" + ver + "\"");
                ver = ver.substring(0, len - 1);
            }

            return ModuleDescriptor.Version.parse(ver).toString();
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Error parsing version info from " + filename + " (" + ver + "): " + e.getMessage(), e);
        }
    }

    private static String cleanModuleName(String mn) {
        // replace non-alphanumeric
        mn = NON_ALPHANUM.matcher(mn).replaceAll(".");

        // collapse repeating dots
        mn = REPEATING_DOTS.matcher(mn).replaceAll(".");

        // drop leading dots
        if (!mn.isEmpty() && mn.charAt(0) == '.')
            mn = LEADING_DOTS.matcher(mn).replaceAll("");

        // drop trailing dots
        int len = mn.length();
        if (len > 0 && mn.charAt(len - 1) == '.')
            mn = TRAILING_DOTS.matcher(mn).replaceAll("");

        // fixup digits-only components
        mn = NUMBERLIKE_PARTS.matcher(mn).replaceAll("_$1");

        // fixup keyword components
        mn = KEYWORD_PARTS.matcher(mn).replaceAll("_$1");

        return mn;
    }

    /**
     * @see JarModuleInfo#scanAutomaticModule
     */
    public static void scanAutomaticModule(
            JarContents jar,
            ModuleDescriptor.Builder builder,
            String... excludedRootDirectories) {
        Set<String> ignoredRootDirs = Set.of(excludedRootDirectories);

        Set<String> packageNames = new HashSet<>();
        Map<String, JarResource> serviceProviderFiles = new HashMap<>();
        jar.visitContent((relativePath, resource) -> {
            // Ignore all content in META-INF except for service files
            if (relativePath.startsWith("META-INF/services/")) {
                String filename = relativePath.substring(relativePath.lastIndexOf('/') + 1);

                // Ignore files in META-INF/services/ whose filenames are not valid Java class names
                if (JlsConstants.isTypeName(filename)) {
                    serviceProviderFiles.put(filename, resource.retain());
                }
            } else {
                // In automatic modules, only packages with .class files are considered,
                // unlike with normal modules where resources would also be scanned.
                if (relativePath.endsWith(".class")) {
                    var lastSeparator = relativePath.lastIndexOf('/');
                    if (lastSeparator > 0) {
                        String relativeDir = relativePath.substring(0, lastSeparator);
                        var packageName = relativeDir.replace('/', '.');
                        if (JlsConstants.isTypeName(packageName)) {
                            packageNames.add(packageName);
                        }
                    }
                }
            }
        });
        builder.packages(packageNames);

        for (var serviceProviderEntry : serviceProviderFiles.entrySet()) {
            var serviceProviderName = serviceProviderEntry.getKey();

            try (var reader = serviceProviderEntry.getValue().bufferedReader(StandardCharsets.UTF_8)) {
                parseServiceFile(serviceProviderName, reader, packageNames, builder);
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to parse service provider file " + serviceProviderName + " in " + jar, e);
            }
        }
    }

    /**
     * Parses a Java ServiceLoader file and adds its content as a provided service to the given module
     * descriptor builder.
     * <p>Equivalent to the code found in ModulePath#deriveModuleDescriptor(JarFile)
     */
    private static void parseServiceFile(String serviceName, BufferedReader reader, Set<String> packageNames, ModuleDescriptor.Builder builder) throws IOException {
        List<String> providerClasses = new ArrayList<>();
        String line;
        while ((line = reader.readLine()) != null) {
            // Strip comments
            var startOfComment = line.indexOf('#');
            if (startOfComment != -1) {
                line = line.substring(0, startOfComment);
            }
            line = line.trim(); // Trim whitespace *after* removing the comment

            // We're parsing service files after scanning for packages,
            // which means we can validate at this point that a service provider
            // only provides a class that is contained in the Jar file.
            if (!line.isEmpty()) {
                String packageName = JlsConstants.getPackageName(line);
                if (!packageNames.contains(packageName)) {
                    String msg = "Service provider file " + serviceName + " contains service that is not in this Jar file: " + line;
                    throw new InvalidModuleDescriptorException(msg);
                }
                providerClasses.add(line);
            }
        }

        if (!providerClasses.isEmpty()) {
            builder.provides(serviceName, providerClasses);
        }
    }

    /**
     * @see JarModuleInfo#scanModulePackages
     */
    public static Set<String> scanModulePackages(JarContents jar) {
        Set<String> packageNames = new HashSet<>();

        jar.visitContent((relativePath, resource) -> {
            var lastSeparator = relativePath.lastIndexOf('/');
            if (lastSeparator < 1) {
                return; // File is in the default package
            }

            var packageName = relativePath.substring(0, lastSeparator).replace('/', '.');
            if (JlsConstants.isTypeName(packageName)) {
                packageNames.add(packageName);
            }
        });

        return Set.copyOf(packageNames);
    }
}
