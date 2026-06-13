/*
 * Copyright (c) Forge Development LLC and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.loading;

import java.nio.file.Files;
import java.nio.file.Path;

public class LibraryFinder {
    static Path findLibsPath() {
        var libraryDirectoryProp = System.getProperty("libraryDirectory");
        if (libraryDirectoryProp == null) {
            throw new IllegalStateException("Missing libraryDirectory system property");
        }
        var libsPath = Path.of(libraryDirectoryProp);
        if (!Files.isDirectory(libsPath)) {
            throw new IllegalStateException("libraryDirectory system property refers to a non-directory: " + libsPath);
        }
        return libsPath;
    }

    public static Path findPathForMaven(String group, String artifact, String extension, String classifier, String version) {
        return findPathForMaven(new MavenCoordinate(group, artifact, extension, classifier, version));
    }

    public static Path findPathForMaven(MavenCoordinate artifact) {
        return findLibsPath().resolve(artifact.toRelativeRepositoryPath());
    }
}
