/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.classloading.transformation;

import com.google.common.graph.GraphBuilder;
import com.google.common.graph.Graphs;
import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.SequencedMap;
import java.util.Set;
import java.util.function.Function;
import net.neoforged.fml.CrashReportCallables;
import net.neoforged.fml.ModLoader;
import net.neoforged.fml.ModLoadingIssue;
import net.neoforged.fml.loading.toposort.TopologicalSort;
import net.neoforged.fml.util.ServiceLoaderUtil;
import net.neoforged.neoforgespi.transformation.BytecodeProvider;
import net.neoforged.neoforgespi.transformation.ClassProcessor;
import net.neoforged.neoforgespi.transformation.ClassProcessorIds;
import net.neoforged.neoforgespi.transformation.ClassProcessorProvider;
import net.neoforged.neoforgespi.transformation.ProcessorName;
import org.jetbrains.annotations.ApiStatus;
import org.objectweb.asm.Type;
import org.slf4j.Logger;

/**
 * A sorted collection of class processors and accompanying metadata that can be linked
 * with a bytecode source to form a class transformer.
 */
@ApiStatus.Internal
public final class ClassProcessorSet {
    private final List<ClassProcessor> sortedProcessors;
    private final Set<ProcessorName> markerProcessors;
    private final Set<String> generatedPackages;
    private final SequencedMap<ProcessorName, ClassProcessor> processors;
    private final Set<ProcessorName> allowedToRecomputeFrames;
    private boolean linked;

    private ClassProcessorSet(List<ClassProcessor> sortedProcessors,
            Set<ProcessorName> markers,
            Set<String> generatedPackages,
            Set<ProcessorName> allowedToRecomputeFrames) {
        CrashReportCallables.registerCrashCallable("Class Processors", () -> ClassTransformStatistics.computeCrashReportEntry(this));
        this.sortedProcessors = List.copyOf(sortedProcessors);
        this.markerProcessors = Set.copyOf(markers);
        this.generatedPackages = Set.copyOf(generatedPackages);
        var processors = LinkedHashMap.<ProcessorName, ClassProcessor>newLinkedHashMap(sortedProcessors.size());
        for (var processor : sortedProcessors) {
            processors.put(processor.name(), processor);
        }
        this.allowedToRecomputeFrames = Set.copyOf(allowedToRecomputeFrames);
        this.processors = Collections.unmodifiableSequencedMap(processors);
    }

    public static ClassProcessorSet of(ClassProcessor... processors) {
        return ClassProcessorSet.builder()
                .addProcessors(Arrays.asList(processors))
                .build();
    }

    boolean canRecomputeFrames(ProcessorName name) {
        return allowedToRecomputeFrames.contains(name);
    }

    public boolean isMarker(ClassProcessor processor) {
        return markerProcessors.contains(processor.name());
    }

    public Set<String> getGeneratedPackages() {
        return generatedPackages;
    }

    List<ClassProcessor> getSortedProcessors() {
        return sortedProcessors;
    }

    public List<ClassProcessor> transformersFor(Type classDesc, boolean isEmpty, ProcessorName upToTransformer) {
        var out = new ArrayList<ClassProcessor>();
        boolean includesComputingFrames = false;
        for (var transformer : sortedProcessors) {
            if (upToTransformer != null && upToTransformer.equals(transformer.name())) {
                break;
            } else if (ClassProcessorIds.COMPUTING_FRAMES.equals(transformer.name())) {
                includesComputingFrames = true;
                out.add(transformer);
            } else {
                ClassTransformStatistics.incrementAskedForTransform(transformer);

                var context = new ClassProcessor.SelectionContext(classDesc, isEmpty);
                if (transformer.handlesClass(context)) {
                    ClassTransformStatistics.incrementTransforms(transformer);
                    out.add(transformer);
                }
            }
        }
        if ((out.size() == 1 && includesComputingFrames)) {
            // The class does not actually require any transformation, as the only transformer present is the special
            // no-op marker for where class hierarchy computation in frame computation goes up to, and potentially the
            // marker for where results are fixed and may be responded to.
            return List.of();
        }
        return out;
    }

    public void link(Function<ProcessorName, BytecodeProvider> bytecodeProviderLookup) {
        if (linked) {
            throw new IllegalStateException("This set of class processors is already linked.");
        }
        linked = true;

        for (var processor : sortedProcessors) {
            var context = new ClassProcessor.LinkContext(processors, bytecodeProviderLookup.apply(processor.name()));
            processor.link(context);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private static final Logger LOGGER = LogUtils.getLogger();

        private final List<ClassProcessor> processors = new ArrayList<>();
        private final Set<ProcessorName> markers = new HashSet<>();

        private Builder() {}

        public Builder markMarker(ProcessorName name) {
            markers.add(name);
            return this;
        }

        public Builder addProcessor(ClassProcessor toAdd) {
            processors.add(toAdd);
            return this;
        }

        public Builder addProcessors(Collection<ClassProcessor> toAdd) {
            processors.addAll(toAdd);
            return this;
        }

        public Builder addProcessorProviders(Collection<ClassProcessorProvider> providers) {
            var context = new ClassProcessorProvider.Context();
            for (var provider : providers) {
                try {
                    provider.createProcessors(context, Builder.this.processors::add);
                } catch (Exception e) {
                    // Throwing here would cause the game to immediately crash without a proper error screen,
                    // since this method is called by ModLauncher directly. We also need to be able to attribute errors to
                    // the actual mod causing them.
                    var sourceFile = ServiceLoaderUtil.identifySourcePath(provider);
                    ModLoader.addLoadingIssue(
                            ModLoadingIssue.error("fml.modloadingissue.coremod_error", provider.getClass().getName(), sourceFile).withCause(e));
                }
            }
            return this;
        }

        @SuppressWarnings("UnstableApiUsage")
        private static List<ClassProcessor> sortProcessors(List<ClassProcessor> allProcessors, Set<ProcessorName> allowedToRecomputeFrames) {
            var transformers = new HashMap<ProcessorName, ClassProcessor>();
            var graph = GraphBuilder.directed().<ClassProcessor>build();

            var specialComputeFramesNode = createSpecialComputeFramesNode();

            graph.addNode(specialComputeFramesNode);
            transformers.put(specialComputeFramesNode.name(), specialComputeFramesNode);
            for (var transformer : allProcessors) {
                if (transformers.containsKey(transformer.name())) {
                    LOGGER.error(
                            "Duplicate transformers with name {}, of types {} and {}",
                            transformer.name(),
                            transformers.get(transformer.name()).getClass().getName(),
                            transformer.getClass().getName());
                    throw new IllegalStateException("Duplicate transformers with name: " + transformer.name());
                }
                graph.addNode(transformer);
                transformers.put(transformer.name(), transformer);
            }
            for (var self : transformers.values()) {
                // If the targeted transformer is not present, then the ordering does not matter;
                // this allows for e.g. ordering with transformers that may or may not be present.
                for (var targetName : self.runsBefore()) {
                    var target = transformers.get(targetName);
                    if (target == self) {
                        continue;
                    }
                    if (target != null) {
                        graph.putEdge(self, target);
                    }
                }
                for (var targetName : self.runsAfter()) {
                    var target = transformers.get(targetName);
                    if (target == self) {
                        continue;
                    }
                    if (target != null) {
                        graph.putEdge(target, self);
                    }
                }
            }
            var sorted = TopologicalSort.topologicalSort(graph, Comparator.comparing(ClassProcessor::orderingHint).thenComparing(ClassProcessor::name));
            for (var node : Graphs.reachableNodes(graph, specialComputeFramesNode)) {
                allowedToRecomputeFrames.add(node.name());
            }
            return sorted;
        }

        private static ClassProcessor createSpecialComputeFramesNode() {
            return new ClassProcessor() {
                @Override
                public ProcessorName name() {
                    return ClassProcessorIds.COMPUTING_FRAMES;
                }

                @Override
                public Set<ProcessorName> runsAfter() {
                    return Set.of();
                }

                @Override
                public OrderingHint orderingHint() {
                    return OrderingHint.EARLY;
                }

                @Override
                public boolean handlesClass(SelectionContext context) {
                    return false;
                }

                @Override
                public ComputeFlags processClass(TransformationContext context) {
                    return ComputeFlags.NO_REWRITE;
                }
            };
        }

        public ClassProcessorSet build() {
            var allowedToRecomputeFrames = new HashSet<ProcessorName>();
            var sortedProcessors = sortProcessors(processors, allowedToRecomputeFrames);

            var packageNames = new HashSet<String>();
            for (var factory : processors) {
                packageNames.addAll(factory.generatesPackages());
            }
            return new ClassProcessorSet(sortedProcessors, markers, packageNames, allowedToRecomputeFrames);
        }
    }
}
