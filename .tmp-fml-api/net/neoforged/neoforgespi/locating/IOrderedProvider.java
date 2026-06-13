/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforgespi.locating;

/**
 * Base interface for providers that support ordering.
 */
public interface IOrderedProvider {
    /**
     * The default priority of providers that otherwise do not specify a priority.
     */
    int DEFAULT_PRIORITY = 0;

    /**
     * The highest priority a providers built into NeoForge will use.
     */
    int HIGHEST_SYSTEM_PRIORITY = 1000;

    /**
     * The lowest priority a providers built into NeoForge will use.
     */
    int LOWEST_SYSTEM_PRIORITY = -1000;

    /**
     * Gets the priority in which this provider will be called.
     * A higher value means the provider will be called earlier.
     */
    default int getPriority() {
        return DEFAULT_PRIORITY;
    }
}
