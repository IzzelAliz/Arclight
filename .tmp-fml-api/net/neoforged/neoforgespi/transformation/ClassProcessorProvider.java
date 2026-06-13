/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforgespi.transformation;

import java.util.ServiceLoader;
import org.jetbrains.annotations.ApiStatus;

/**
 * A provider can be implemented and exposed using the Java {@link ServiceLoader} to contribute
 * {@linkplain ClassProcessor class processors} to the launching game. You can also provide {@link ClassProcessor}
 * implementations using service loader directly, but this interface allows the launch environment to be inspected
 * and processors to be added conditionally.
 */
public interface ClassProcessorProvider {
    @ApiStatus.NonExtendable
    interface Collector {
        void add(ClassProcessor processor);
    }

    /**
     * Context about the currently launching game that is passed when processors are collected.
     */
    record Context() {
        @ApiStatus.Internal
        public Context {}
    }

    /**
     * Called by the loader when it collects all class processors to apply
     * to the game. Providers must add any processors they want to
     * activate for the currently launching game to {@code collector}.
     */
    void createProcessors(Context context, Collector collector);
}
