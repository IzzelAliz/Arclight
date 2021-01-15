package io.izzel.arclight.common.asm;

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
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class InventoryImplementer implements Implementer {

    private static final Marker MARKER = MarkerManager.getMarker("INVENTORY");
    private static final String INV_TYPE = "net/minecraft/inventory/IInventory";
    private static final String BRIDGE_TYPE = "io/izzel/arclight/common/bridge/inventory/IInventoryBridge";

    private final Map<String, Boolean> map = new ConcurrentHashMap<>();

    public InventoryImplementer() {
        map.put(INV_TYPE, true);
        map.put("java/lang/Object", false);
    }

    @Override
    public boolean processClass(ClassNode node, ILaunchPluginService.ITransformerLoader transformerLoader) {
        try {
            if (node.interfaces.contains(BRIDGE_TYPE)) {
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
        if (node.interfaces.contains(INV_TYPE)) {
            map.put(node.name, true);
            return true;
        } else {
            Boolean b = map.get(node.superName);
            if (b != null) {
                map.put(node.name, b);
                return b;
            } else {
                return isInventoryClass(findClass(node.superName, transformerLoader), transformerLoader);
            }
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
            if (desc.equals("func_70297_j_()I")) {
                stackLimitMethod = method;
            }
        }
        if (methods.contains("getViewers()Ljava/util/List;")) {
            ArclightImplementer.LOGGER.debug(MARKER, "Found implemented class {}", node.name);
            node.interfaces.add(BRIDGE_TYPE);
            return false;
        } else {
            List<FieldNode> list = findPossibleList(node);
            if (list.size() != 1) {
                if (list.size() > 1) {
                    ArclightImplementer.LOGGER.warn(MARKER, "Found multiple possible fields in class {}: {}", node.name, list.stream().map(it -> it.name + it.desc).collect(Collectors.joining(", ")));
                } else return false;
            }
            ArclightImplementer.LOGGER.debug(MARKER, "Implementing inventory for class {}", node.name);
            FieldNode stackList = list.get(0);
            FieldNode transaction = new FieldNode(Opcodes.ACC_PRIVATE, "transaction", Type.getType(List.class).getDescriptor(), null, null);
            FieldNode maxStack = new FieldNode(Opcodes.ACC_PRIVATE, "maxStack", "I", null, 64);
            node.fields.add(transaction);
            node.fields.add(maxStack);
            node.interfaces.add(BRIDGE_TYPE);
            InsnList initInsn = new InsnList();
            {
                MethodNode methodNode = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC, "getContents", Type.getMethodDescriptor(Type.getType(List.class)), null, null);
                InsnList insnList = new InsnList();
                insnList.add(new VarInsnNode(Opcodes.ALOAD, 0));
                insnList.add(new FieldInsnNode(Opcodes.GETFIELD, node.name, stackList.name, stackList.desc));
                insnList.add(new InsnNode(Opcodes.ARETURN));
                methodNode.instructions = insnList;
                node.methods.add(methodNode);
            }
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
            {
                MethodNode methodNode = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC, "func_70297_j_", "()I", null, null);
                InsnList insnList = new InsnList();
                insnList.add(new VarInsnNode(Opcodes.ALOAD, 0));
                insnList.add(new FieldInsnNode(Opcodes.GETFIELD, node.name, maxStack.name, maxStack.desc));
                insnList.add(new InsnNode(Opcodes.IRETURN));
                methodNode.instructions = insnList;
                node.methods.add(methodNode);
            }
            if (stackLimitMethod == null) {
                initInsn.add(new VarInsnNode(Opcodes.ALOAD, 0));
                initInsn.add(new IntInsnNode(Opcodes.BIPUSH, 64));
                initInsn.add(new FieldInsnNode(Opcodes.PUTFIELD, node.name, maxStack.name, maxStack.desc));
            } else {
                stackLimitMethod.name += "$" + Integer.toHexString(ThreadLocalRandom.current().nextInt());
                initInsn.add(new VarInsnNode(Opcodes.ALOAD, 0));
                initInsn.add(new InsnNode(Opcodes.DUP));
                initInsn.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, node.name, stackLimitMethod.name, stackLimitMethod.desc, false));
                initInsn.add(new FieldInsnNode(Opcodes.PUTFIELD, node.name, maxStack.name, maxStack.desc));
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
                        MethodNode mn = new MethodNode();
                        initInsn.accept(mn);
                        InsnList insnList = mn.instructions;
                        insnList.add(new VarInsnNode(Opcodes.ALOAD, 0));
                        insnList.add(new TypeInsnNode(Opcodes.NEW, Type.getInternalName(ArrayList.class)));
                        insnList.add(new InsnNode(Opcodes.DUP));
                        insnList.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, Type.getInternalName(ArrayList.class), "<init>", "()V", false));
                        insnList.add(new FieldInsnNode(Opcodes.PUTFIELD, node.name, transaction.name, transaction.desc));
                        methodNode.instructions.insert(initNode, insnList);
                    }
                }
            }
            return true;
        }
    }

    private List<FieldNode> findPossibleList(ClassNode node) {
        LinkedList<FieldNode> list = new LinkedList<>();
        for (FieldNode fieldNode : node.fields) {
            boolean nonNullList = fieldNode.desc.equals("Lnet/minecraft/util/NonNullList;");
            if (nonNullList || fieldNode.desc.equals("Ljava/util/List;")) {
                if (fieldNode.signature != null && fieldNode.signature.contains("<Lnet/minecraft/item/ItemStack;>")) {
                    if (nonNullList) {
                        list.addFirst(fieldNode);
                    } else {
                        list.addLast(fieldNode);
                    }
                }
            }
        }
        return list;
    }
}
