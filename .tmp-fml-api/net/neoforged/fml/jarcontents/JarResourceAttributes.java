/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.jarcontents;

import java.nio.file.attribute.FileTime;

/**
 * Metadata attributes of a {@link JarResource}.
 *
 * @param lastModified The last modification time of the resource.
 * @param size         The file size of the resource in bytes.
 */
public record JarResourceAttributes(FileTime lastModified, long size) {}
