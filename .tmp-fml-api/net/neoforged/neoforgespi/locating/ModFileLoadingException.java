/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforgespi.locating;

/**
 * An exception that can be thrown/caught by {@link IModFileCandidateLocator} code, indicating a bad mod file or something similar.
 */
public class ModFileLoadingException extends RuntimeException {
    public ModFileLoadingException(String message) {
        super(message);
    }

    public ModFileLoadingException(String message, Throwable cause) {
        super(message, cause);
    }
}
