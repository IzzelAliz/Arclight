/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.common.asm.enumextension;

import org.jetbrains.annotations.Nullable;

/**
 * Provides information on whether an enum was extended and how many entries are vanilla vs. modded
 *
 * @param extended     Whether this enum had additional entries added to it
 * @param vanillaCount How many entries the enum originally contained, 0 if the enum is not extended
 * @param totalCount   How many entries the enum contains after extension, 0 if the enum is not extended
 * @param netCheck     Whether the enum needs to be checked for network compatibility
 */
public record ExtensionInfo(boolean extended, int vanillaCount, int totalCount, @Nullable NetworkedEnum.NetworkCheck netCheck) {
    public static <T extends Enum<T> & IExtensibleEnum> ExtensionInfo nonExtended(Class<T> enumClass) {
        NetworkedEnum anno = enumClass.getDeclaredAnnotation(NetworkedEnum.class);
        return new ExtensionInfo(false, 0, 0, anno == null ? null : anno.value());
    }
}
