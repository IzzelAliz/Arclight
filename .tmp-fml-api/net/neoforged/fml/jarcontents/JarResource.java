/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.jarcontents;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import org.jetbrains.annotations.ApiStatus;

/**
 * A resource found in a {@link JarContents}.
 *
 * <p><strong>NOTE:</strong> Instances of this class obtained from {@link JarResourceVisitor} should not be
 * copied for use outside the visitor. If you need to hold onto a resource outside the visitor, copy it first
 * using {@link #retain()}.
 */
@ApiStatus.NonExtendable
public interface JarResource {
    InputStream open() throws IOException;

    default BufferedReader bufferedReader() throws IOException {
        return bufferedReader(StandardCharsets.UTF_8);
    }

    default BufferedReader bufferedReader(Charset charset) throws IOException {
        return new BufferedReader(new InputStreamReader(open(), charset));
    }

    default byte[] readAllBytes() throws IOException {
        try (var stream = open()) {
            return stream.readAllBytes();
        }
    }

    /**
     * Reads metadata attributes of this resource.
     *
     * @return The attributes of this resource.
     * @throws IOException If accessing the attributes fails due to an I/O error.
     */
    JarResourceAttributes attributes() throws IOException;

    /**
     * Create a copy of this jar resource reference that can be held onto.
     * <p>Useful in the context of {@link JarResourceVisitor}, where resource objects are reused between visits,
     * and copies must be made to hold onto resources for later use.
     */
    JarResource retain();
}
