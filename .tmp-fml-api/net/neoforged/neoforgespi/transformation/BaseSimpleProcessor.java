/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforgespi.transformation;

import java.util.Set;

/**
 * Base class for simple {@linkplain ClassProcessor class processors}.
 */
abstract sealed class BaseSimpleProcessor implements ClassProcessor permits SimpleClassProcessor, SimpleFieldProcessor, SimpleMethodProcessor {
    /**
     * Linking is an advanced operation and not supported by simple processors.
     */
    @Override
    public final void link(LinkContext context) {
        ClassProcessor.super.link(context);
    }

    /**
     * After processing callbacks are an advanced operation not supported by simple processors.
     */
    @Override
    public final void afterProcessing(AfterProcessingContext context) {
        ClassProcessor.super.afterProcessing(context);
    }

    @Override
    public Set<ProcessorName> runsAfter() {
        return Set.of(ClassProcessorIds.SIMPLE_PROCESSORS_GROUP, ClassProcessorIds.COMPUTING_FRAMES);
    }
}
