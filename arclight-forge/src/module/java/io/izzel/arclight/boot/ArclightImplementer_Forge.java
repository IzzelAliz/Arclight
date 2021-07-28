package io.izzel.arclight.boot;

import cpw.mods.modlauncher.api.NamedPath;
import cpw.mods.modlauncher.serviceapi.ILaunchPluginService;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

import java.util.EnumSet;
import java.util.function.Consumer;

public class ArclightImplementer_Forge implements ILaunchPluginService {

    private final ILaunchPluginService delegate;

    public ArclightImplementer_Forge() {
        var module = getClass().getModule();
        for (var m : module.getLayer().modules()) {
            module.addReads(m);
        }
        for (var layer : module.getLayer().parents()) {
            layer.modules().forEach(module::addReads);
        }
        try {
            delegate = (ILaunchPluginService) Class.forName("io.izzel.arclight.common.asm.ArclightImplementer")
                .getConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String name() {
        return delegate.name();
    }

    @Override
    public EnumSet<Phase> handlesClass(Type classType, boolean isEmpty) {
        return delegate.handlesClass(classType, isEmpty);
    }

    @Override
    public EnumSet<Phase> handlesClass(Type classType, boolean isEmpty, String reason) {
        return delegate.handlesClass(classType, isEmpty, reason);
    }

    @Override
    public boolean processClass(Phase phase, ClassNode classNode, Type classType, String reason) {
        return delegate.processClass(phase, classNode, classType, reason);
    }

    @Override
    public void initializeLaunch(ITransformerLoader transformerLoader, NamedPath[] specialPaths) {
        delegate.initializeLaunch(transformerLoader, specialPaths);
    }

    @Override
    public void customAuditConsumer(String className, Consumer<String[]> auditDataAcceptor) {
        delegate.customAuditConsumer(className, auditDataAcceptor);
    }
}
