/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.common.asm;

import org.jetbrains.annotations.ApiStatus;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;

@ApiStatus.Internal
public class ListGeneratorAdapter extends GeneratorAdapter {
    public final InsnList insnList;

    public ListGeneratorAdapter(InsnList insnList) {
        super(Opcodes.ASM9, null, 0, "", "()V");
        this.insnList = insnList;
        MethodNode method = new MethodNode();
        method.instructions = insnList;
        mv = method;
    }
}
