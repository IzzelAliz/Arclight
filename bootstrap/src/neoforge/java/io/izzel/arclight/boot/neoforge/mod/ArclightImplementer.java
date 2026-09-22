package io.izzel.arclight.boot.neoforge.mod;

import io.izzel.arclight.boot.asm.*;
import net.neoforged.neoforgespi.transformation.ClassProcessor;
import net.neoforged.neoforgespi.transformation.ClassProcessorIds;
import net.neoforged.neoforgespi.transformation.ProcessorName;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class ArclightImplementer implements ClassProcessor {

    private final Map<String, Implementer> implementers = new LinkedHashMap<>();
    private final boolean logger;

    public ArclightImplementer() {
        this(detectTransformLogger());
    }

    public ArclightImplementer(boolean logger) {
        this.logger = logger;
    }

    private static boolean detectTransformLogger() {
        var transformLogger = !(java.util.logging.LogManager.getLogManager() instanceof org.apache.logging.log4j.jul.LogManager);
        if (transformLogger && !System.getProperties().containsKey("log4j.jul.LoggerAdapter")) {
            System.setProperty("log4j.jul.LoggerAdapter", "io.izzel.arclight.boot.log.ArclightLoggerAdapter");
        }
        return transformLogger;
    }

    @Override
    public ProcessorName name() {
        return new ProcessorName("arclight", "implementer");
    }

    @Override
    public Set<ProcessorName> runsAfter() {
        // COMPUTING_FRAMES is a hierarchy-query boundary, not a final writer pass.
        // FML only permits COMPUTE_FRAMES results from processors after this marker.
        // Mixin's bytecode queries stop before MIXIN, so they cannot re-enter us.
        return Set.of(ClassProcessorIds.MIXIN, ClassProcessorIds.COMPUTING_FRAMES);
    }

    @Override
    public void link(LinkContext context) {
        implementers.put("inventory", new InventoryImplementer());
        implementers.put("switch", SwitchTableFixer.INSTANCE);
        implementers.put("async", AsyncCatcher.INSTANCE);
        implementers.put("enum", new EnumDefinalizer());
        if (logger) {
            implementers.put("logger", new LoggerTransformer());
        }
    }

    @Override
    public boolean handlesClass(SelectionContext context) {
        return !context.empty();
    }

    @Override
    public ComputeFlags processClass(TransformationContext context) {
        if (context.empty()) return ComputeFlags.NO_REWRITE;
        boolean changed = false;
        for (var entry : implementers.entrySet()) {
            if (entry.getValue().processClass(context.node())) {
                context.audit(entry.getKey());
                changed = true;
            }
        }
        // Async/inventory/switch transformations add branches, stack entries and methods.
        return changed ? ComputeFlags.COMPUTE_FRAMES : ComputeFlags.NO_REWRITE;
    }
}
