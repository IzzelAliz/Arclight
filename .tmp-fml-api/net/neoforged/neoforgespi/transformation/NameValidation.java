/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforgespi.transformation;

import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;

final class NameValidation {
    private NameValidation() {}

    static void validateClassName(String name) {
        ClassDesc.of(name);
    }

    static void validateUnqualified(String name) {
        ".;[/<>".chars().forEach(c -> {
            if (name.indexOf(c) != -1) {
                throw new IllegalArgumentException("Invalid unqualified name " + name);
            }
        });
    }

    static void validateMethod(String name, String descriptor) {
        if (name.equals("<init>") || (name.equals("<clinit>") && descriptor.equals("()V"))) {
            return;
        }
        validateUnqualified(name);
        MethodTypeDesc.ofDescriptor(descriptor);
    }
}
