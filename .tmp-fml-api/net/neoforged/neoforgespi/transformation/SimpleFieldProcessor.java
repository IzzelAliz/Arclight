/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.neoforgespi.transformation;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import org.objectweb.asm.tree.FieldNode;

public abstract non-sealed class SimpleFieldProcessor extends BaseSimpleProcessor {
    private final AtomicReference<Map<String, Set<String>>> targetsByClass = new AtomicReference<>();

    /**
     * Applies transformations to a {@linkplain #targets() targeted field}.
     *
     * @param input The ASM input node, which can be mutated directly
     */
    public abstract void transform(FieldNode input, SimpleTransformationContext context);

    /**
     * {@return the fields targeted by this processor}
     */
    public abstract Set<Target> targets();

    /**
     * Identifies a targeted field and the class it resides in.
     *
     * @param className the binary name of the class containing the field, as {@link Class#getName()}
     * @param fieldName the name of the field
     */
    public record Target(String className, String fieldName) {
        public Target {
            NameValidation.validateClassName(className);
            NameValidation.validateUnqualified(fieldName);
        }
    }

    private Map<String, Set<String>> targetsByClass() {
        return this.targetsByClass.updateAndGet(
                map -> map != null ? map
                        : targets().stream().collect(
                                Collectors.groupingBy(Target::className, Collectors.mapping(
                                        Target::fieldName,
                                        Collectors.toSet()))));
    }

    @Override
    public final boolean handlesClass(SelectionContext context) {
        return targetsByClass().containsKey(context.type().getClassName());
    }

    @Override
    public final ComputeFlags processClass(TransformationContext context) {
        var targetFields = this.targetsByClass().get(context.type().getClassName());
        if (targetFields == null) {
            return ComputeFlags.NO_REWRITE;
        }

        boolean transformed = false;
        for (var field : context.node().fields) {
            if (targetFields.contains(field.name)) {
                transform(field, context);
                transformed = true;
            }
        }

        return transformed ? ComputeFlags.COMPUTE_FRAMES : ComputeFlags.NO_REWRITE;
    }
}
