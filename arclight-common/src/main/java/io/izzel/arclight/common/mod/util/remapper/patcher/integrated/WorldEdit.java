package io.izzel.arclight.common.mod.util.remapper.patcher.integrated;

import io.izzel.arclight.api.PluginPatcher;
import io.izzel.arclight.common.mod.server.ArclightServer;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.util.HashMap;
import java.util.Map;

public class WorldEdit {

    // Correct usage of Paper's CraftBukkit methods in BukkitAdapter
    public static void handleBukkitAdapter(ClassNode node, PluginPatcher.ClassRepo repo) {
        MethodNode adaptBlockState = null;
        for (MethodNode method : node.methods) {
            if ("adapt".equals(method.name) &&
                    Type.getReturnType(method.desc).getInternalName().equals("com/sk89q/worldedit/world/block/BlockState")) {
                adaptBlockState = method;
                break;
            }
        }
        if (adaptBlockState == null) {
            throw new UnsupportedOperationException("Cannot find adapt(...):BlockState in PaperweightAdapter");
        }
        boolean success = false;
        for (AbstractInsnNode insn : adaptBlockState.instructions) {
            if (insn instanceof MethodInsnNode invocation && "createData".equals(invocation.name)) {
                invocation.name = "fromData";
                success = true;
                break;
            }
        }
        if (!success) {
            throw new UnsupportedOperationException("Cannot find CraftBlockData#createData invocation in PaperweightAdapter");
        }
    }

    // Correctly handle reflection name picking
    // Their naming mapping for NMS is somehow behind the version
    public static void handleStaticRefraction(ClassNode node, PluginPatcher.ClassRepo repo) {
        var override = Map.of("NEXT_TICK_TIME", "nextTickTimeNanos");
        for (MethodNode method : node.methods) {
            if ("<clinit>".equals(method.name)) {
                LdcInsnNode lastLdc = null;
                Map<String, String> fieldToProvided = new HashMap<>();
                for(var insn: method.instructions) {
                    if (insn instanceof LdcInsnNode ldc && ldc.cst instanceof String) {
                        lastLdc = ldc;
                    }
                    if (insn instanceof FieldInsnNode field) {
                        fieldToProvided.put(field.name, (String) lastLdc.cst);
                    }
                }
                method.instructions.clear();

                int line = 0;
                for (var entry: fieldToProvided.entrySet()) {
                    var label = new LabelNode();
                    method.instructions.add(label);
                    method.instructions.add(new LineNumberNode(--line, label));
                    method.instructions.add(new LdcInsnNode(override.getOrDefault(entry.getKey(), entry.getValue())));
                    method.instructions.add(new FieldInsnNode(
                            Opcodes.PUTSTATIC,
                            node.name,
                            entry.getKey(),
                            "Ljava/lang/String;"
                    ));
                }

                var label = new LabelNode();
                method.instructions.add(label);
                method.instructions.add(new LineNumberNode(--line, label));
                method.instructions.add(new FieldInsnNode(Opcodes.GETSTATIC, Type.getInternalName(ArclightServer.class), "LOGGER", Type.getDescriptor(Logger.class)));
                method.instructions.add(new InsnNode(Opcodes.DUP));
                method.instructions.add(new LdcInsnNode("patcher.integrated.we-enable"));
                method.instructions.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE, Type.getInternalName(Logger.class), "warn", "(Ljava/lang/String;)V", true));
                method.instructions.add(new LdcInsnNode("patcher.integrated.we-warning"));
                method.instructions.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE, Type.getInternalName(Logger.class), "warn", "(Ljava/lang/String;)V", true));

                method.instructions.add(new InsnNode(Opcodes.RETURN));

                method.visitMaxs(2, 0);
            }
        }
    }
}