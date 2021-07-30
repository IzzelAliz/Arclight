package io.izzel.arclight.boot.asm;

import com.google.common.io.ByteStreams;
import cpw.mods.modlauncher.serviceapi.ILaunchPluginService;
import io.izzel.arclight.api.ArclightVersion;
import io.izzel.arclight.i18n.LocalizedException;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class InventoryImplementer implements Implementer {

    private static final Marker MARKER = MarkerManager.getMarker("INVENTORY");
    private static final String INV_TYPE = "net/minecraft/world/Container";
    private static final String BRIDGE_TYPE = "io/izzel/arclight/common/bridge/core/inventory/IInventoryBridge";

    private final Map<String, Integer> map = new ConcurrentHashMap<>();

    public InventoryImplementer() {
        map.put(INV_TYPE, 1);
        map.put("java/lang/Object", 0);
    }

    @Override
    public boolean processClass(ClassNode node, ILaunchPluginService.ITransformerLoader transformerLoader) {
        try {
            if (Modifier.isInterface(node.access) || node.interfaces.contains(BRIDGE_TYPE)) {
                return false;
            }
            if (isInventoryClass(node, transformerLoader)) {
                return tryImplement(node);
            } else return false;
        } catch (Throwable t) {
            if (t instanceof LocalizedException) {
                ArclightImplementer.LOGGER.error(MARKER, ((LocalizedException) t).node(), ((LocalizedException) t).args());
            } else {
                ArclightImplementer.LOGGER.error(t);
            }
            return false;
        }
    }

    private boolean isInventoryClass(ClassNode node, ILaunchPluginService.ITransformerLoader transformerLoader) throws Throwable {
        Integer ret = map.get(node.name);
        if (ret != null) return ret > 1;
        Integer i = map.get(node.superName);
        if (i != null) {
            if (i > 1) {
                map.put(node.name, i + 1);
                return true;
            }
        }
        if (node.interfaces.contains(INV_TYPE)) {
            map.put(node.name, 2);
            return true;
        } else {
            boolean b = isInventoryClass(findClass(node.superName, transformerLoader), transformerLoader);
            if (b) {
                map.put(node.name, map.get(node.superName) + 1);
            } else {
                map.put(node.name, 0);
            }
            return b;
        }
    }

    private ClassNode findClass(String typeName, ILaunchPluginService.ITransformerLoader transformerLoader) throws Exception {
        try {
            InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(typeName + ".class");
            if (stream == null) throw LocalizedException.checked("implementer.not-found", typeName);
            byte[] array = ByteStreams.toByteArray(stream);
            ClassNode node = new ClassNode();
            new ClassReader(array).accept(node, ClassReader.SKIP_CODE);
            return node;
        } catch (Throwable e) {
            try {
                byte[] bytes = transformerLoader.buildTransformedClassNodeFor(Type.getObjectType(typeName).getClassName());
                ClassNode node = new ClassNode();
                new ClassReader(bytes).accept(node, ClassReader.SKIP_CODE);
                return node;
            } catch (Throwable t) {
                throw LocalizedException.checked("implementer.not-found", typeName);
            }
        }
    }

    private boolean tryImplement(ClassNode node) {
        Set<String> methods = new HashSet<>();
        MethodNode stackLimitMethod = null;
        for (MethodNode method : node.methods) {
            String desc = method.name + method.desc;
            methods.add(desc);
            if (desc.equals("m_6893_()I")) {
                stackLimitMethod = method;
            }
        }
        if (methods.contains("getViewers()Ljava/util/List;")) {
            ArclightImplementer.LOGGER.debug(MARKER, "Found implemented class {}", node.name);
            node.interfaces.add(BRIDGE_TYPE);
            return false;
        } else {
            ArclightImplementer.LOGGER.debug(MARKER, "Implementing inventory for class {}", node.name);
            FieldNode transaction = new FieldNode(Opcodes.ACC_PRIVATE, "$transaction", Type.getType(List.class).getDescriptor(), null, null);
            FieldNode maxStack = new FieldNode(Opcodes.ACC_PRIVATE, "$maxStack", "I", null, null);
            node.fields.add(transaction);
            node.fields.add(maxStack);
            node.interfaces.add(BRIDGE_TYPE);
            {
                MethodNode methodNode = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC, "onOpen", Type.getMethodDescriptor(Type.VOID_TYPE, Type.getObjectType("org/bukkit/craftbukkit/" + ArclightVersion.current().packageName() + "/entity/CraftHumanEntity")), null, null);
                InsnList insnList = new InsnList();
                insnList.add(new VarInsnNode(Opcodes.ALOAD, 0));
                insnList.add(new FieldInsnNode(Opcodes.GETFIELD, node.name, transaction.name, transaction.desc));
                insnList.add(new VarInsnNode(Opcodes.ALOAD, 1));
                insnList.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE, Type.getInternalName(List.class), "add", "(Ljava/lang/Object;)Z", true));
                insnList.add(new InsnNode(Opcodes.POP));
                insnList.add(new InsnNode(Opcodes.RETURN));
                methodNode.instructions = insnList;
                node.methods.add(methodNode);
            }
            {
                MethodNode methodNode = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC, "onClose", Type.getMethodDescriptor(Type.VOID_TYPE, Type.getObjectType("org/bukkit/craftbukkit/" + ArclightVersion.current().packageName() + "/entity/CraftHumanEntity")), null, null);
                InsnList insnList = new InsnList();
                insnList.add(new VarInsnNode(Opcodes.ALOAD, 0));
                insnList.add(new FieldInsnNode(Opcodes.GETFIELD, node.name, transaction.name, transaction.desc));
                insnList.add(new VarInsnNode(Opcodes.ALOAD, 1));
                insnList.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE, Type.getInternalName(List.class), "remove", "(Ljava/lang/Object;)Z", true));
                insnList.add(new InsnNode(Opcodes.POP));
                insnList.add(new InsnNode(Opcodes.RETURN));
                methodNode.instructions = insnList;
                node.methods.add(methodNode);
            }
            {
                MethodNode methodNode = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC, "getViewers", Type.getMethodDescriptor(Type.getType(List.class)), null, null);
                InsnList insnList = new InsnList();
                insnList.add(new VarInsnNode(Opcodes.ALOAD, 0));
                insnList.add(new FieldInsnNode(Opcodes.GETFIELD, node.name, transaction.name, transaction.desc));
                insnList.add(new InsnNode(Opcodes.ARETURN));
                methodNode.instructions = insnList;
                node.methods.add(methodNode);
            }
            InsnList list = new InsnList();
            LabelNode labelNode = new LabelNode();
            list.add(new VarInsnNode(Opcodes.ALOAD, 0));
            list.add(new FieldInsnNode(Opcodes.GETFIELD, node.name, maxStack.name, maxStack.desc));
            list.add(new InsnNode(Opcodes.ICONST_M1));
            list.add(new JumpInsnNode(Opcodes.IF_ICMPEQ, labelNode));
            list.add(new VarInsnNode(Opcodes.ALOAD, 0));
            list.add(new FieldInsnNode(Opcodes.GETFIELD, node.name, maxStack.name, maxStack.desc));
            list.add(new InsnNode(Opcodes.IRETURN));
            list.add(labelNode);
            if (stackLimitMethod != null && !Modifier.isAbstract(stackLimitMethod.access)) {
                stackLimitMethod.instructions.insert(list);
            } else {
                MethodNode methodNode = stackLimitMethod == null
                    ? new MethodNode(0, "m_6893_()I", "()I", null, null)
                    : stackLimitMethod;
                methodNode.access = Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC;
                int level = map.get(node.name);
                list.add(new VarInsnNode(Opcodes.ALOAD, 0));
                if (level > 2) {
                    list.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, node.superName, methodNode.name, methodNode.desc, false));
                } else {
                    list.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, INV_TYPE, methodNode.name, methodNode.desc, true));
                }
                list.add(new InsnNode(Opcodes.IRETURN));
                methodNode.instructions.insert(list);
                node.methods.add(methodNode);
            }
            {
                MethodNode methodNode = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC, "setMaxStackSize", "(I)V", null, null);
                InsnList insnList = new InsnList();
                insnList.add(new VarInsnNode(Opcodes.ALOAD, 0));
                insnList.add(new VarInsnNode(Opcodes.ILOAD, 1));
                insnList.add(new FieldInsnNode(Opcodes.PUTFIELD, node.name, maxStack.name, maxStack.desc));
                insnList.add(new InsnNode(Opcodes.RETURN));
                methodNode.instructions = insnList;
                node.methods.add(methodNode);
            }
            {
                for (MethodNode methodNode : node.methods) {
                    if (methodNode.name.equals("<init>")) {
                        AbstractInsnNode initNode = methodNode.instructions.getFirst();
                        while (!(initNode.getOpcode() == Opcodes.INVOKESPECIAL && ((MethodInsnNode) initNode).name.equals("<init>"))) {
                            initNode = initNode.getNext();
                        }
                        InsnList insnList = new InsnList();
                        insnList.add(new VarInsnNode(Opcodes.ALOAD, 0));
                        insnList.add(new TypeInsnNode(Opcodes.NEW, Type.getInternalName(ArrayList.class)));
                        insnList.add(new InsnNode(Opcodes.DUP));
                        insnList.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, Type.getInternalName(ArrayList.class), "<init>", "()V", false));
                        insnList.add(new FieldInsnNode(Opcodes.PUTFIELD, node.name, transaction.name, transaction.desc));
                        insnList.add(new VarInsnNode(Opcodes.ALOAD, 0));
                        insnList.add(new InsnNode(Opcodes.ICONST_M1));
                        insnList.add(new FieldInsnNode(Opcodes.PUTFIELD, node.name, maxStack.name, maxStack.desc));
                        methodNode.instructions.insert(initNode, insnList);
                    }
                }
            }
            return true;
        }
    }
}
