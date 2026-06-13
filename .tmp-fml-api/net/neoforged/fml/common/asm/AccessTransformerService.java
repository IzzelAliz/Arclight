/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.common.asm;

import java.util.Set;
import net.neoforged.accesstransformer.api.AccessTransformerEngine;
import net.neoforged.neoforgespi.transformation.ClassProcessor;
import net.neoforged.neoforgespi.transformation.ClassProcessorIds;
import net.neoforged.neoforgespi.transformation.ProcessorName;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public class AccessTransformerService implements ClassProcessor {
    private final AccessTransformerEngine engine;

    public AccessTransformerService(AccessTransformerEngine engine) {
        this.engine = engine;
    }

    @Override
    public ProcessorName name() {
        return ClassProcessorIds.ACCESS_TRANSFORMERS;
    }

    @Override
    public Set<ProcessorName> runsBefore() {
        return Set.of(ClassProcessorIds.MIXIN);
    }

    @Override
    public ComputeFlags processClass(TransformationContext context) {
        return engine.transform(context.node(), context.type()) ? ComputeFlags.SIMPLE_REWRITE : ComputeFlags.NO_REWRITE;
    }

    @Override
    public boolean handlesClass(SelectionContext context) {
        return !context.empty() && engine.getTargets().contains(context.type());
    }
}
