/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforgespi.transformation;

/**
 * Allows class processors to access bytecode of classes other than the class that is currently being transformed.
 */
public interface BytecodeProvider {
    /**
     * Gets the bytecode for a given class. This bytecode is provided in the form that the processor would see,
     * were it transforming that class; any transformers ordered before will have been applied to it already.
     *
     * @param className the class to locate, in dot-separated form
     * @return the bytecode
     * @throws ClassNotFoundException if the class cannot be found
     */
    byte[] getByteCode(String className) throws ClassNotFoundException;
}
