/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml;

public final class FMLVersion {
    private FMLVersion() {}

    public static String getVersion() {
        return FMLVersionProperties.VERSION;
    }
}
