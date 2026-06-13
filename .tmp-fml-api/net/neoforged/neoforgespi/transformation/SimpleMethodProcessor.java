/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforgespi.transformation;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import org.objectweb.asm.tree.MethodNode;

/**
 * Base class for simple {@link ClassProcessor} implementations that want to apply
 * bytecode transformations to specific methods.
 */
public abstract non-sealed class SimpleMethodProcessor extends BaseSimpleProcessor {
    private final AtomicReference<Map<String, Set<String>>> targetsByClass = new AtomicReference<>();

    /**
     * Applies transformations to a {@linkplain #targets() targeted method}.
     *
     * @param input The ASM input node, which can be mutated directly
     */
    public abstract void transform(MethodNode input, SimpleTransformationContext context);

    /**
     * {@return the methods targeted by this processor}
     */
    public abstract Set<Target> targets();

    /**
     * Identifies a targeted method and the class it resides in.
     *
     * @param className        the binary name of the class containing the method, as {@link Class#getName()}
     * @param methodName       the name of the method
     * @param methodDescriptor the method's descriptor string
     */
    public record Target(String className, String methodName, String methodDescriptor) {
        public Target {
            NameValidation.validateClassName(className);
            NameValidation.validateMethod(methodName, methodDescriptor);
        }
    }

    private Map<String, Set<String>> targetsByClass() {
        return this.targetsByClass.updateAndGet(
                map -> map != null ? map
                        : targets().stream().collect(
                                Collectors.groupingBy(Target::className, Collectors.mapping(
                                        t -> t.methodName() + t.methodDescriptor(),
                                        Collectors.toSet()))));
    }

    @Override
    public final boolean handlesClass(SelectionContext context) {
        return targetsByClass().containsKey(context.type().getClassName());
    }

    @Override
    public final ComputeFlags processClass(TransformationContext context) {
        var targetMethods = this.targetsByClass().get(context.type().getClassName());
        if (targetMethods == null) {
            return ComputeFlags.NO_REWRITE;
        }

        boolean transformed = false;
        for (var method : context.node().methods) {
            if (targetMethods.contains(method.name + method.desc)) {
                transform(method, context);
                transformed = true;
            }
        }

        return transformed ? ComputeFlags.COMPUTE_FRAMES : ComputeFlags.NO_REWRITE;
    }
}
