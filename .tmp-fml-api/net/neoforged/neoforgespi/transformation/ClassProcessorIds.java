/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforgespi.transformation;

/**
 * The names of built-in class processors.
 */
public final class ClassProcessorIds {
    /**
     * A dummy processor used to order processors relative to frame computation; anything that requires frame
     * re-computation should run after this, and anything providing information that should be available for frame
     * computation should run before this. Thus, any processor that returns {@link ClassProcessor.ComputeFlags#COMPUTE_FRAMES}
     * <em>must</em> run after this processor.
     */
    public static final ProcessorName COMPUTING_FRAMES = new ProcessorName("neoforge", "computing_frames");
    /**
     * A dummy processor acting as a default group for processors provided by {@link SimpleClassProcessor}, {@link SimpleMethodProcessor}, and {@link SimpleFieldProcessor}.
     */
    public static final ProcessorName SIMPLE_PROCESSORS_GROUP = new ProcessorName("neoforge", "simple_processors_default");
    public static final ProcessorName RUNTIME_ENUM_EXTENDER = new ProcessorName("neoforge", "runtime_enum_extender");
    public static final ProcessorName ACCESS_TRANSFORMERS = new ProcessorName("neoforge", "access_transformer");
    public static final ProcessorName MIXIN = new ProcessorName("neoforge", "mixin");
    public static final ProcessorName DIST_CLEANER = new ProcessorName("neoforge", "neoforge_dev_dist_cleaner");

    private ClassProcessorIds() {}
}
