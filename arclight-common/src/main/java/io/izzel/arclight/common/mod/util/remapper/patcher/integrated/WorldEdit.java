package io.izzel.arclight.common.mod.util.remapper.patcher.integrated;

import io.izzel.arclight.api.PluginPatcher;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.util.Locale;

public class WorldEdit {

    public static void handleBukkitAdapter(ClassNode node, PluginPatcher.ClassRepo repo) {
        MethodNode standardize = new MethodNode(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC, "patcher$standardize",
            Type.getMethodDescriptor(Type.getType(String.class), Type.getType(String.class)), null, null);
        try {
            GeneratorAdapter adapter = new GeneratorAdapter(standardize, standardize.access, standardize.name, standardize.desc);
            adapter.loadArg(0);
            adapter.push(':');
            adapter.push('_');
            adapter.invokeVirtual(Type.getType(String.class), Method.getMethod(String.class.getMethod("replace", char.class, char.class)));
            adapter.push("\\s+");
            adapter.push("_");
            adapter.invokeVirtual(Type.getType(String.class), Method.getMethod(String.class.getMethod("replaceAll", String.class, String.class)));
            adapter.push("\\W");
            adapter.push("");
            adapter.invokeVirtual(Type.getType(String.class), Method.getMethod(String.class.getMethod("replaceAll", String.class, String.class)));
            adapter.getStatic(Type.getType(Locale.class), "ENGLISH", Type.getType(Locale.class));
            adapter.invokeVirtual(Type.getType(String.class), Method.getMethod(String.class.getMethod("toUpperCase", Locale.class)));
            adapter.returnValue();
            adapter.endMethod();
        } catch (Throwable t) {
            t.printStackTrace();
        }
        node.methods.add(standardize);
        for (MethodNode method : node.methods) {
            if (method.name.equals("adapt")) {
                handleAdapt(node, standardize, method);
            }
        }
    }

    public static void handlePickName(ClassNode node, PluginPatcher.ClassRepo repo) {
        for (MethodNode method : node.methods) {
            if (method.name.equals("pickName")) {
                method.instructions.clear();
                method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
                method.instructions.add(new InsnNode(Opcodes.ARETURN));
                return;
            }
        }
    }

    private static void handleAdapt(ClassNode node, MethodNode standardize, MethodNode method) {
        switch (method.desc) {
            case "(Lcom/sk89q/worldedit/world/item/ItemType;)Lorg/bukkit/Material;":
            case "(Lcom/sk89q/worldedit/world/block/BlockType;)Lorg/bukkit/Material;":
            case "(Lcom/sk89q/worldedit/world/biome/BiomeType;)Lorg/bukkit/block/Biome;":
            case "(Lcom/sk89q/worldedit/world/entity/EntityType;)Lorg/bukkit/entity/EntityType;": {
                for (AbstractInsnNode instruction : method.instructions) {
                    if (instruction.getOpcode() == Opcodes.ATHROW) {
                        InsnList list = new InsnList();
                        list.add(new VarInsnNode(Opcodes.ALOAD, 0));
                        list.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, Type.getMethodType(method.desc).getArgumentTypes()[0].getInternalName(), "getId", "()Ljava/lang/String;", false));
                        list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, node.name, standardize.name, standardize.desc, false));
                        switch (Type.getMethodType(method.desc).getReturnType().getInternalName()) {
                            case "org/bukkit/Material":
                                list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "org/bukkit/Material", "getMaterial", "(Ljava/lang/String;)Lorg/bukkit/Material;", false));
                                break;
                            case "org/bukkit/block/Biome":
                                list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "org/bukkit/block/Biome", "valueOf", "(Ljava/lang/String;)Lorg/bukkit/block/Biome;", false));
                                break;
                            case "org/bukkit/entity/EntityType":
                                list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "org/bukkit/entity/EntityType", "fromName", "(Ljava/lang/String;)Lorg/bukkit/entity/EntityType;", false));
                                break;
                        }
                        list.add(new InsnNode(Opcodes.ARETURN));
                        method.instructions.insert(instruction, list);
                        method.instructions.set(instruction, new InsnNode(Opcodes.POP));
                        return;
                    }
                }
                break;
            }
        }
    }

    public static void handleWatchdog(ClassNode node, PluginPatcher.ClassRepo repo) {
        if (node.interfaces.size() == 1 && node.interfaces.get(0).equals("com/sk89q/worldedit/extension/platform/Watchdog")
            && node.name.contains("SpigotWatchdog")) {
            for (MethodNode method : node.methods) {
                if (method.name.equals("<init>")) {
                    method.instructions.clear();
                    method.instructions.add(new TypeInsnNode(Opcodes.NEW, "java/lang/ClassNotFoundException"));
                    method.instructions.add(new InsnNode(Opcodes.DUP));
                    method.instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/ClassNotFoundException", "<init>", "()V", false));
                    method.instructions.add(new InsnNode(Opcodes.ATHROW));
                    method.tryCatchBlocks.clear();
                    method.localVariables.clear();
                    return;
                }
            }
        }
    }
}