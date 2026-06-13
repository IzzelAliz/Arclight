/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.common.asm.enumextension;

import java.util.List;
import org.objectweb.asm.Type;

sealed interface EnumParameters {
    record Constant(List<Object> params) implements EnumParameters {}

    record FieldReference(Type owner, String fieldName) implements EnumParameters {}

    record MethodReference(Type owner, String methodName) implements EnumParameters {}
}
