/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.common.asm.enumextension;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Marks a constructor that is reserved for vanilla enum constants and cannot be used to create
 * additional entries through the enum extension system
 */
@Target(ElementType.CONSTRUCTOR)
public @interface ReservedConstructor {}
