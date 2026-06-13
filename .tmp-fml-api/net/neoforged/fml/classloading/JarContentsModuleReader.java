/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.classloading;

import java.io.IOException;
import java.io.InputStream;
import java.lang.module.ModuleReader;
import java.net.URI;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.stream.Stream;
import net.neoforged.fml.jarcontents.JarContents;

final class JarContentsModuleReader implements ModuleReader {
    private final JarContents contents;

    public JarContentsModuleReader(JarContents contents) {
        this.contents = contents;
    }

    @Override
    public Optional<InputStream> open(String name) throws IOException {
        return Optional.ofNullable(contents.openFile(name));
    }

    @Override
    public Optional<URI> find(String name) {
        return contents.findFile(name);
    }

    @Override
    public Stream<String> list() {
        var content = new LinkedHashSet<String>();
        contents.visitContent((relativePath, resource) -> content.add(relativePath));
        return content.stream();
    }

    @Override
    public void close() {}

    @Override
    public String toString() {
        return contents.toString();
    }
}
