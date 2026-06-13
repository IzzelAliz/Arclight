/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.common.asm;

import java.util.Set;
import net.neoforged.neoforgespi.transformation.ClassProcessor;
import net.neoforged.neoforgespi.transformation.ClassProcessorIds;
import net.neoforged.neoforgespi.transformation.ProcessorName;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public class SimpleProcessorsGroup implements ClassProcessor {
    // For ordering purposes only; allows making transformers that run before/after all "default" coremods
    @Override
    public ProcessorName name() {
        return ClassProcessorIds.SIMPLE_PROCESSORS_GROUP;
    }

    @Override
    public Set<ProcessorName> runsAfter() {
        return Set.of(ClassProcessorIds.COMPUTING_FRAMES, ClassProcessorIds.MIXIN);
    }

    @Override
    public boolean handlesClass(SelectionContext context) {
        return false;
    }

    @Override
    public ComputeFlags processClass(TransformationContext context) {
        return ComputeFlags.NO_REWRITE;
    }
}
