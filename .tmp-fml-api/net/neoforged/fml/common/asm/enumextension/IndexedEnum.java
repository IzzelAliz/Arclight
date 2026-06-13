/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.common.asm.enumextension;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Specifies that the annotated enum has an int ID parameter which must match the enum constant's ordinal
 */
@Target(ElementType.TYPE)
public @interface IndexedEnum {
    /**
     * {@return the parameter index of the ID parameter}
     */
    int value() default 0;
}
