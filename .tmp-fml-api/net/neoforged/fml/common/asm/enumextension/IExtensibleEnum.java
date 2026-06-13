/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.common.asm.enumextension;

/**
 * To be implemented on vanilla enums that should be enhanced with ASM to be
 * extensible. If this is implemented on a class, the class must define a static
 * method called {@code getExtensionInfo()} which takes zero args and returns an {@link ExtensionInfo}.
 * By default, the method must return {@link ExtensionInfo#nonExtended(Class)} with the enum's class as the parameter.
 *
 * {@snippet :
 * public static ExtensionInfo getExtensionInfo() {
 *     return ExtensionInfo.nonExtended(TheEnum.class);
 * }
 * }
 *
 * The method contents will be replaced with ASM at runtime to return information about the extension of the enum
 * <p>
 * Enum constants added to enums implementing this interface can be retrieved later via {@code TheEnum.valueOf(String)}
 * or from the associated {@link EnumProxy} field if the parameters were supplied through it
 */
public interface IExtensibleEnum {}
