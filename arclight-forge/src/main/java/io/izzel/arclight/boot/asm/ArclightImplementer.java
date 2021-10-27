package io.izzel.arclight.boot.asm;

import cpw.mods.modlauncher.api.NamedPath;
import cpw.mods.modlauncher.serviceapi.ILaunchPluginService;
import io.izzel.arclight.boot.log.ArclightI18nLogger;
import io.izzel.arclight.boot.mod.ModBootstrap;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
        this(false);
    }

    public ArclightImplementer(boolean logger) {
        this.logger = logger;
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
        this.implementers.put("entitytype", EntityTypePatcher.INSTANCE);
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
            if (implementer.processClass(classNode, transformerLoader)) {
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

    public static void loadArgs(InsnList list, MethodNode methodNode, Type[] types, int i) {
        if (!Modifier.isStatic(methodNode.access)) {
            list.add(new VarInsnNode(Opcodes.ALOAD, i));
            i += 1;
        }
        for (Type type : types) {
            list.add(new VarInsnNode(type.getOpcode(Opcodes.ILOAD), i));
            i += type.getSize();
        }
    }
}
