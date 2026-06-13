/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.loading.mixin;

import java.util.Set;
import net.neoforged.neoforgespi.transformation.ClassProcessor;
import net.neoforged.neoforgespi.transformation.ClassProcessorIds;
import net.neoforged.neoforgespi.transformation.ProcessorName;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.injection.invoke.arg.ArgsClassGenerator;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;
import org.spongepowered.asm.service.ISyntheticClassRegistry;

public class FMLMixinClassProcessor implements ClassProcessor {
    private final FMLAuditTrail auditTrail;
    private final FMLClassTracker classTracker;
    private final IMixinTransformer transformer;
    private final ISyntheticClassRegistry registry;
    private final FMLMixinService service;

    public FMLMixinClassProcessor(FMLMixinService service) {
        this.auditTrail = service.getInternalAuditTrail();
        this.classTracker = service.getInternalClassTracker();
        this.transformer = service.getMixinTransformer();
        this.registry = transformer.getExtensions().getSyntheticClassRegistry();
        this.service = service;
    }

    @Override
    public void link(LinkContext context) {
        this.service.setBytecodeProvider(new FMLClassBytecodeProvider(context.bytecodeProvider(), this));
    }

    @Override
    public ProcessorName name() {
        return ClassProcessorIds.MIXIN;
    }

    @Override
    public Set<String> generatesPackages() {
        return Set.of(ArgsClassGenerator.SYNTHETIC_PACKAGE);
    }

    @Override
    public boolean handlesClass(SelectionContext context) {
        // Throw if the class was previously determined to be invalid
        String name = context.type().getClassName();
        if (this.classTracker.isInvalidClass(name)) {
            throw new NoClassDefFoundError(String.format("%s is invalid", name));
        }

        if (!context.empty()) {
            if (processesClass(context.type())) {
                return true;
            }
            // If there is no chance of the class being processed, we do not bother.
        }

        if (this.transformer.getExtensions().getSyntheticClassRegistry() == null) {
            return false;
        }

        return this.generatesClass(context.type());
    }

    private boolean processesClass(Type classType) {
        MixinEnvironment environment = MixinEnvironment.getCurrentEnvironment();
        return this.transformer.couldTransformClass(environment, classType.getClassName());
    }

    boolean generatesClass(Type classType) {
        return this.registry.findSyntheticClass(classType.getClassName()) != null;
    }

    boolean generateClass(Type classType, ClassNode classNode) {
        return this.transformer.generateClass(MixinEnvironment.getCurrentEnvironment(), classType.getClassName(), classNode);
    }

    @Override
    public ComputeFlags processClass(TransformationContext context) {
        var classType = context.type();
        var classNode = context.node();

        this.auditTrail.setConsumer(classType.getClassName(), context::audit);

        if (this.generatesClass(classType)) {
            return this.generateClass(classType, classNode) ? ComputeFlags.COMPUTE_FRAMES : ComputeFlags.NO_REWRITE;
        }

        MixinEnvironment environment = MixinEnvironment.getCurrentEnvironment();
        return this.transformer.transformClass(environment, classType.getClassName(), classNode) ? ComputeFlags.COMPUTE_FRAMES : ComputeFlags.NO_REWRITE;
    }

    @Override
    public void afterProcessing(AfterProcessingContext context) {
        // Mixin wants to know when a class is _loaded_ for its internal tracking (to avoid allowing newly-loaded mixins,
        // since mixins can be loaded at arbitrary times, to affect already-loaded classes), but processors can
        // technically run on classes more than once (if, say, another transform requests the state before that
        // transform would run). Hence, why we are running in a post-result callback, which is guaranteed to be called
        // once, right before class load.
        this.classTracker.addLoadedClass(context.type().getClassName());
    }
}
