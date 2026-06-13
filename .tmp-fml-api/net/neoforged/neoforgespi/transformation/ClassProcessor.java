/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforgespi.transformation;

import java.util.SequencedMap;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Unmodifiable;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

/**
 * Class processors provide an API for transforming classes as they are loaded. They take care to use correctly and
 * efficiently. The main pieces of a processor are {@link #handlesClass(SelectionContext)} and
 * {@link #processClass(TransformationContext)}, which allow processors to say whether they want to process a given
 * class and allow them to transform the class. Processors are named and should have sensible namespaces; ordering is
 * accomplished by specifying names that processors should run before or after if present.
 * <p>
 * For a simpler API, see {@link SimpleClassProcessor}, {@link SimpleMethodProcessor}, and {@link SimpleFieldProcessor}.
 * <p>Class processors can be provided using the Java {@link java.util.ServiceLoader} mechanism. If you
 * require more control, you can also implement a {@link ClassProcessorProvider}.
 */
public interface ClassProcessor {
    String GENERATED_PACKAGE_MODULE = "net.neoforged.fml.generated";

    /**
     * {@return a unique identifier for this processor}
     */
    ProcessorName name();

    /**
     * {@return processors that this processor must run before}
     */
    default Set<ProcessorName> runsBefore() {
        return Set.of();
    }

    /**
     * {@return processors that this processor must run after} This should include
     * {@link ClassProcessorIds#COMPUTING_FRAMES} if the processor returns a result requiring frame re-computation.
     */
    default Set<ProcessorName> runsAfter() {
        return Set.of(ClassProcessorIds.COMPUTING_FRAMES);
    }

    /**
     * {@return packages that this processor generates classes for, that do not already exist on the game layer}
     * Generated packages in the game layer will be in the module {@value ClassProcessor#GENERATED_PACKAGE_MODULE}.
     */
    default Set<String> generatesPackages() {
        return Set.of();
    }

    /**
     * {@return a hint for how this processor should be ordered relative to other processors} Note that this is a
     * comparatively weak hint; {@link #runsBefore()} and {@link #runsAfter()} take precedence, and processors don't
     * have "phases" of any sort.
     */
    default OrderingHint orderingHint() {
        return OrderingHint.DEFAULT;
    }

    enum OrderingHint {
        EARLY,
        DEFAULT,
        LATE
    }

    /**
     * {@return whether the processor wants to recieve the class}
     *
     * @param context the context of the class to consider
     */
    boolean handlesClass(SelectionContext context);

    /**
     * Each class that the processor has opted to recieve is passed to this method for processing.
     *
     * @param context the context of the class to process
     * @return the {@link ClassProcessor.ComputeFlags} indicating how the class should be rewritten.
     */
    ComputeFlags processClass(TransformationContext context);

    /**
     * Where a class may be processed multiple times by the same processor (for example, if in addition to being loaded
     * a later processor requests the state of the given class using a {@link BytecodeProvider}), an after-processing
     * callback is guaranteed to run at most once per class, just before class load. A transformer will only see this
     * context for classes it has processed.
     *
     * @param context the context of the class that was processed
     */
    default void afterProcessing(AfterProcessingContext context) {}

    /**
     * Called once after a set of class processors has been linked together
     * with a bytecode source. FML will not call transformation methods
     * on this class processor, until it has been linked.
     * <p>FML only links providers once during startup.
     */
    default void link(LinkContext context) {}

    enum ComputeFlags {
        /**
         * This plugin did not change the class and therefor requires no rewrite of the class.
         * This is the fastest option
         */
        NO_REWRITE,
        /**
         * The plugin did change the class and requires a rewrite, but does not require any additional computation
         * as frames and maxs in the class did not change or have been corrected by the plugin.
         */
        SIMPLE_REWRITE,
        /**
         * The plugin did change the class and requires a rewrite, and requires max re-computation,
         * but frames are unchanged or corrected by the plugin
         */
        COMPUTE_MAXS,
        /**
         * The plugin did change the class and requires a rewrite, and requires frame re-computation.
         * This is the slowest, but also the safest method if you don't know what level is required.
         * This implies {@link #COMPUTE_MAXS}, so maxs will also be recomputed.
         */
        COMPUTE_FRAMES;

        /**
         * {@return which of the two flags is a superset of the other}
         * 
         * @param other the other flag to compare against
         */
        public ComputeFlags max(ComputeFlags other) {
            return this.ordinal() > other.ordinal() ? this : other;
        }
    }

    /**
     * Context available when determining whether a processor wants to handle a class
     * 
     * @param type  the class to consider
     * @param empty if the class is empty at present (indicates no backing file found)
     */
    record SelectionContext(Type type, boolean empty) {
        @ApiStatus.Internal
        public SelectionContext {}
    }

    /**
     * Context available when processing a class
     */
    final class TransformationContext implements SimpleTransformationContext {
        private final Type type;
        private final ClassNode node;
        private final boolean empty;
        private final BiConsumer<String, String[]> auditTrail;
        private final Supplier<byte[]> initialSha256;

        @ApiStatus.Internal
        public TransformationContext(Type type, ClassNode node, boolean empty, BiConsumer<String, String[]> auditTrail, Supplier<byte[]> initialSha256) {
            this.type = type;
            this.node = node;
            this.empty = empty;
            this.auditTrail = auditTrail;
            this.initialSha256 = initialSha256;
        }

        /**
         * {@return} the type of the class being transformed
         */
        public Type type() {
            return type;
        }

        /**
         * {@return the class being transformed} Modifications will be reflected in the output bytecode (and the loaded class).
         */
        public ClassNode node() {
            return node;
        }

        /**
         * {@return whether the class was empty when provided to this transformer} Note that a class might not exist on
         * disk but still return {@code false} here, if an earlier transformer provided it.
         */
        public boolean empty() {
            return empty;
        }

        /**
         * Add audit activity for this transformation.
         * 
         * @param activity what was done to the class
         * @param context  any additional information to include
         */
        public void audit(String activity, String... context) {
            auditTrail.accept(activity, context);
        }

        /**
         * {@return the SHA-256 hash of the original class bytecode}
         */
        public byte[] initialSha256() {
            return initialSha256.get();
        }
    }

    /**
     * Context available in post-processing single-instance callbacks
     * 
     * @param type the class that was processed
     */
    record AfterProcessingContext(Type type) {
        @ApiStatus.Internal
        public AfterProcessingContext {}
    }

    /**
     * The context provided to {@linkplain ClassProcessor class processors}
     * when they are linked with a bytecode source.
     * 
     * @param processors       an immutable map of the processors that are being linked. The maps iteration order is the order in which the processors will be applied
     * @param bytecodeProvider a provider to access class bytecode for other classes than the class that is being transformed
     */
    record LinkContext(
            @Unmodifiable SequencedMap<ProcessorName, ClassProcessor> processors,
            BytecodeProvider bytecodeProvider) {
        @ApiStatus.Internal
        public LinkContext {}
    }
}
