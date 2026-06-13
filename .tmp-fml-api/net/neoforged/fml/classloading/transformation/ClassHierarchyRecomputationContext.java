/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.classloading.transformation;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * Provides information required to compute class hierarchies when writing out a transformed classes bytecode, while
 * recomputing stack frames.
 * <p>
 * Every class name passed to methods of this interface use dot-separated form.
 */
@ApiStatus.Internal
public interface ClassHierarchyRecomputationContext {
    /**
     * {@return the class identified by className, if it's already loaded and visible from the current classloader or null , if it's not}
     */
    @Nullable
    Class<?> findLoadedClass(String className);

    /**
     * Gets the class bytecode of any reachable class. If the class is subject to transformation,
     * any class processors that require frame recomputation will already be applied.
     */
    byte[] upToFrames(String className) throws ClassNotFoundException;

    /**
     * Loads and returns a class by name, if it's not subject to transformation.
     */
    Class<?> locateParentClass(String className) throws ClassNotFoundException;
}
