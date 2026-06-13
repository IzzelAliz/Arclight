/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.loading;

import net.neoforged.api.distmarker.Dist;

/**
 * This is a convenience class for quickly accessing various aspects of the {@linkplain FMLLoader#getCurrent()} current loader}.
 * <p>If no {@link FMLLoader} is currently active, all methods in this class will throw.
 */
public final class FMLEnvironment {
    private FMLEnvironment() {}

    /**
     * @see FMLLoader#getDist()
     */
    public static Dist getDist() {
        return FMLLoader.getCurrent().getDist();
    }

    /**
     * @see FMLLoader#isProduction()
     */
    public static boolean isProduction() {
        return FMLLoader.getCurrent().isProduction();
    }
}
