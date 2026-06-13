/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.common.asm.enumextension;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Specifies that the annotated enum has a string name parameter which must be prefixed with the mod ID
 * of the mod adding a given enum constant
 */
@Target(ElementType.TYPE)
public @interface NamedEnum {
    /**
     * {@return the parameter index of the name parameter}
     */
    int value() default 0;
}
