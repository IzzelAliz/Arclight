package io.izzel.arclight.boot.neoforge.mod;

import io.izzel.arclight.boot.asm.*;
import io.izzel.arclight.boot.log.ArclightI18nLogger;
import net.neoforged.neoforgespi.transformation.ClassProcessor;
import net.neoforged.neoforgespi.transformation.ClassProcessorIds;
import net.neoforged.neoforgespi.transformation.ProcessorName;
import org.apache.logging.log4j.Logger;

import java.util.*;

public class ArclightClassProcessor implements ClassProcessor {

    private static final ProcessorName NAME = new ProcessorName("arclight", "implementer");
    static final Logger LOGGER = ArclightI18nLogger.getLogger("Implementer");

    private final Map<String, Implementer> implementers = new HashMap<>();
    private final boolean logger;

    public ArclightClassProcessor() {
        this(detectTransformLogger());
    }

    ArclightClassProcessor(boolean logger) {
        this.logger = logger;
    }

    private static boolean detectTransformLogger() {
        var transformLogger = !(java.util.logging.LogManager.getLogManager() instanceof org.apache.logging.log4j.jul.LogManager);
        if (transformLogger && !System.getProperties().contains("log4j.jul.LoggerAdapter")) {
            System.setProperty("log4j.jul.LoggerAdapter", "io.izzel.arclight.boot.log.ArclightLoggerAdapter");
        }
        return transformLogger;
    }

    @Override
    public ProcessorName name() {
        return NAME;
    }

    @Override
    public Set<ProcessorName> runsBefore() {
        return Set.of(ClassProcessorIds.MIXIN);
    }

    @Override
    public void link(LinkContext context) {
        ModBootstrap.postRun();
        this.implementers.put("inventory", new InventoryImplementer());
        this.implementers.put("switch", SwitchTableFixer.INSTANCE);
        this.implementers.put("async", AsyncCatcher.INSTANCE);
        this.implementers.put("enum", new EnumDefinalizer());
        if (this.logger) {
            this.implementers.put("logger", new LoggerTransformer());
        }
    }

    @Override
    public boolean handlesClass(SelectionContext context) {
        return !context.empty();
    }

    @Override
    public ComputeFlags processClass(TransformationContext context) {
        var classNode = context.node();
        List<String> trails = new ArrayList<>();
        for (Map.Entry<String, Implementer> entry : implementers.entrySet()) {
            if (entry.getValue().processClass(classNode)) {
                trails.add(entry.getKey());
            }
        }
        if (!trails.isEmpty()) {
            context.audit(String.join(",", trails));
            return ComputeFlags.COMPUTE_FRAMES;
        }
        return ComputeFlags.NO_REWRITE;
    }
}
