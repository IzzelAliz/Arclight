package io.izzel.arclight.common.mod.util.remapper;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.NamespacedKey;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

// 你好
// 不要抄（笑）
@SuppressWarnings("unused")
public class ArclightEnumExtender {

    private static final Logger LOGGER = LogManager.getLogger("EnumExtender");

    public static void process(ClassNode node, List<String> names) {
        String desc = Type.getObjectType(node.name).getDescriptor();
        FieldNode values = tryGetEnumArray(node);
        values.access &= ~Opcodes.ACC_FINAL;
        Set<String> set = countEnum(node);
        tryCreateCtor(node);
        int count = set.size();
        for (MethodNode method : node.methods) {
            if (method.name.equals("<clinit>")) {
                InsnList list = new InsnList();
                InsnList postList = new InsnList();
                for (String name : names) {
                    boolean found = false;
                    if (name.startsWith(NamespacedKey.MINECRAFT + ":")) {
                        if (!set.contains(standardize(name.substring(NamespacedKey.MINECRAFT.length() + 1)))) {
                            LOGGER.warn("Expect {} found in {}, but not", name, node.name);
                        } else found = true;
                    }
                    if (!found) {
                        name = standardize(name);
                        FieldNode fieldNode = new FieldNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL | Opcodes.ACC_ENUM, name, desc, null, null);
                        node.fields.add(fieldNode);
                        list.add(new TypeInsnNode(Opcodes.NEW, node.name));
                        list.add(new InsnNode(Opcodes.DUP));
                        list.add(new LdcInsnNode(name));
                        list.add(loadInt(count));
                        list.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, node.name, "<init>", "(Ljava/lang/String;I)V", false));
                        list.add(new FieldInsnNode(Opcodes.PUTSTATIC, node.name, name, desc));
                        postList.add(new InsnNode(Opcodes.DUP));
                        postList.add(loadInt(count));
                        postList.add(new FieldInsnNode(Opcodes.GETSTATIC, node.name, name, desc));
                        postList.add(new InsnNode(Opcodes.AASTORE));
                        LOGGER.info("Added {} to {}", name, node.name);
                    }
                    count++;
                }
                list.add(new FieldInsnNode(Opcodes.GETSTATIC, node.name, values.name, values.desc));
                list.add(loadInt(0));
                list.add(loadInt(count));
                list.add(new TypeInsnNode(Opcodes.ANEWARRAY, node.name));
                list.add(new InsnNode(Opcodes.DUP));
                list.add(new FieldInsnNode(Opcodes.PUTSTATIC, node.name, values.name, values.desc));
                list.add(loadInt(0));
                list.add(loadInt(set.size()));
                list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/System", "arraycopy", "(Ljava/lang/Object;ILjava/lang/Object;II)V"));
                list.add(new FieldInsnNode(Opcodes.GETSTATIC, node.name, values.name, values.desc));
                postList.add(new InsnNode(Opcodes.POP));
                for (AbstractInsnNode insnNode : method.instructions) {
                    if (insnNode.getOpcode() == Opcodes.RETURN) {
                        method.instructions.insertBefore(insnNode, list);
                        method.instructions.insertBefore(insnNode, postList);
                    }
                }
            }
        }
    }

    private static AbstractInsnNode loadInt(int i) {
        if (i >= -1 && i < 6) {
            return new InsnNode(Opcodes.ICONST_0 + i);
        } else if (i >= -128 && i < 128) {
            return new IntInsnNode(Opcodes.BIPUSH, i);
        } else if (i >= -32768 && i < 32768) {
            return new IntInsnNode(Opcodes.SIPUSH, i);
        } else {
            return new LdcInsnNode(i);
        }
    }

    private static void tryCreateCtor(ClassNode node) {
        boolean found = false;
        for (MethodNode method : node.methods) {
            if (method.name.equals("<init>") && method.desc.equals("(Ljava/lang/String;I)V")) {
                found = true;
                break;
            }
        }
        if (!found) {
            MethodNode methodNode = new MethodNode(Opcodes.ACC_PRIVATE | Opcodes.ACC_SYNTHETIC, "<init>", "(Ljava/lang/String;I)V", null, null);
            InsnList list = new InsnList();
            list.add(new VarInsnNode(Opcodes.ALOAD, 0));
            list.add(new VarInsnNode(Opcodes.ALOAD, 1));
            list.add(new VarInsnNode(Opcodes.ILOAD, 2));
            list.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/Enum", "<init>", "(Ljava/lang/String;I)V", false));
            list.add(new InsnNode(Opcodes.RETURN));
            methodNode.instructions = list;
            node.methods.add(methodNode);
        }
    }

    private static String standardize(String str) {
        return str
            .replace(':', '_')
            .replaceAll("\\s+", "_")
            .replaceAll("\\W", "")
            .toUpperCase(Locale.ENGLISH);
    }

    private static Set<String> countEnum(ClassNode node) {
        Set<String> ret = new HashSet<>();
        for (FieldNode field : node.fields) {
            if ((field.access & Opcodes.ACC_ENUM) != 0) {
                ret.add(field.name);
            }
        }
        return ret;
    }

    private static FieldNode tryGetEnumArray(ClassNode node) {
        String desc = '[' + Type.getObjectType(node.name).getDescriptor();
        List<FieldNode> candidates = new ArrayList<>();
        for (FieldNode field : node.fields) {
            if (Modifier.isStatic(field.access) && field.desc.equals(desc)) {
                candidates.add(field);
            }
        }
        if (candidates.size() != 1) {
            throw new RuntimeException("No $VALUES candidate found in enum class " + node.name);
        }
        return candidates.get(0);
    }
}
