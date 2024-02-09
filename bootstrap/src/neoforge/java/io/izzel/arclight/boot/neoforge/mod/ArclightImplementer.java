package io.izzel.arclight.boot.neoforge.mod;

import cpw.mods.modlauncher.api.NamedPath;
import cpw.mods.modlauncher.serviceapi.ILaunchPluginService;
import io.izzel.arclight.boot.asm.*;
import io.izzel.arclight.boot.log.ArclightI18nLogger;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

import java.util.*;
import java.util.function.Consumer;

public class ArclightImplementer implements ILaunchPluginService {

    static final Logger LOGGER = ArclightI18nLogger.getLogger("Implementer");

    private static final EnumSet<Phase> OH_YES_SIR = EnumSet.of(Phase.AFTER);
    private static final EnumSet<Phase> NOT_TODAY = EnumSet.noneOf(Phase.class);

    private final Map<String, Implementer> implementers = new HashMap<>();
    private volatile Consumer<String[]> auditAcceptor;
    private ITransformerLoader transformerLoader;
    private final boolean logger;

    public ArclightImplementer() {
        this(detectTransformLogger());
    }

    public ArclightImplementer(boolean logger) {
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
    public String name() {
        return "arclight_implementer";
    }

    @Override
    public void initializeLaunch(ITransformerLoader transformerLoader, NamedPath[] specialPaths) {
        // runs after TX CL built
        ModBootstrap.postRun();
        this.transformerLoader = transformerLoader;
        this.implementers.put("inventory", new InventoryImplementer());
        this.implementers.put("switch", SwitchTableFixer.INSTANCE);
        this.implementers.put("async", AsyncCatcher.INSTANCE);
        this.implementers.put("enum", new EnumDefinalizer());
        if (this.logger) {
            this.implementers.put("logger", new LoggerTransformer());
        }
    }

    @Override
    public EnumSet<Phase> handlesClass(Type classType, boolean isEmpty, String reason) {
        if ("mixin".equals(reason)) {
            return NOT_TODAY;
        }
        return isEmpty ? NOT_TODAY : OH_YES_SIR;
    }

    @Override
    public EnumSet<Phase> handlesClass(Type classType, boolean isEmpty) {
        throw new IllegalStateException("Outdated ModLauncher");
    }

    @Override
    public void customAuditConsumer(String className, Consumer<String[]> auditDataAcceptor) {
        auditAcceptor = auditDataAcceptor;
    }

    @Override
    public boolean processClass(Phase phase, ClassNode classNode, Type classType, String reason) {
        if ("mixin".equals(reason)) {
            return false;
        }
        List<String> trails = new ArrayList<>();
        for (Map.Entry<String, Implementer> entry : implementers.entrySet()) {
            String key = entry.getKey();
            Implementer implementer = entry.getValue();
            if (implementer.processClass(classNode)) {
                trails.add(key);
            }
        }
        if (this.auditAcceptor != null && !trails.isEmpty()) {
            this.auditAcceptor.accept(new String[]{String.join(",", trails)});
        }
        return !trails.isEmpty();
    }

    @Override
    public boolean processClass(Phase phase, ClassNode classNode, Type classType) {
        throw new IllegalStateException("Outdated ModLauncher");
    }
}
