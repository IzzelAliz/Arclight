package io.izzel.arclight.boot.asm;

import cpw.mods.modlauncher.serviceapi.ILaunchPluginService;
import org.apache.logging.log4j.jul.LogManager;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;

import java.util.logging.Logger;

public class LoggerTransformer implements Implementer {

    private static final LogManager JUL_MANAGER = new LogManager();

    @Override
    public boolean processClass(ClassNode node, ILaunchPluginService.ITransformerLoader transformerLoader) {
        var transform = false;
        for (var mn : node.methods) {
            for (var insn : mn.instructions) {
                if (insn.getOpcode() == Opcodes.INVOKESTATIC && insn instanceof MethodInsnNode method
                    && method.owner.equals("java/util/logging/Logger") && method.name.equals("getLogger")) {
                    method.owner = Type.getInternalName(LoggerTransformer.class);
                    transform = true;
                }
            }
        }
        return transform;
    }

    public static Logger getLogger(String name) {
        return JUL_MANAGER.getLogger(name);
    }

    public static Logger getLogger(String name, String rb) {
        return JUL_MANAGER.getLogger(name);
    }
}
