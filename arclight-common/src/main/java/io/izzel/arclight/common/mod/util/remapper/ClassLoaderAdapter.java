package io.izzel.arclight.common.mod.util.remapper;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import cpw.mods.modlauncher.api.LamdbaExceptionUtils;
import io.izzel.arclight.common.mod.ArclightMod;
import io.izzel.arclight.common.mod.util.remapper.generated.RemappingURLClassLoader;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
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

import java.lang.invoke.MethodType;
import java.net.URLClassLoader;
import java.nio.ByteBuffer;
import java.security.SecureClassLoader;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ClassLoaderAdapter implements PluginTransformer {

    public static final ClassLoaderAdapter INSTANCE = new ClassLoaderAdapter();
    private static final Marker MARKER = MarkerManager.getMarker("CLADAPTER");
    private static final String CLASSLOADER = "java/lang/ClassLoader";

    private final Map<Type, Map.Entry<Type, int[]>> defineClassTypes = ImmutableMap.<Type, Map.Entry<Type, int[]>>builder()
        .put(Type.getMethodType("(Ljava/lang/String;[BIILjava/security/CodeSource;)Ljava/lang/Class;"), Maps.immutableEntry(Type.getType(SecureClassLoader.class), new int[]{1, 3}))
        .put(Type.getMethodType("(Ljava/lang/String;Ljava/nio/ByteBuffer;Ljava/security/CodeSource;)Ljava/lang/Class;"), Maps.immutableEntry(Type.getType(SecureClassLoader.class), new int[]{1, -1}))
        .put(Type.getMethodType("([BII)Ljava/lang/Class;"), Maps.immutableEntry(Type.getType(ClassLoader.class), new int[]{0, 2}))
        .put(Type.getMethodType("(Ljava/lang/String;[BII)Ljava/lang/Class;"), Maps.immutableEntry(Type.getType(ClassLoader.class), new int[]{1, 3}))
        .put(Type.getMethodType("(Ljava/lang/String;[BIILjava/security/ProtectionDomain;)Ljava/lang/Class;"), Maps.immutableEntry(Type.getType(ClassLoader.class), new int[]{1, 3}))
        .put(Type.getMethodType("(Ljava/lang/String;Ljava/nio/ByteBuffer;Ljava/security/ProtectionDomain;)Ljava/lang/Class;"), Maps.immutableEntry(Type.getType(ClassLoader.class), new int[]{1, -1}))
        .build();
    private final Map<String, String> classLoaderTypes = ImmutableMap.<String, String>builder()
        .put(Type.getInternalName(URLClassLoader.class), Type.getInternalName(RemappingURLClassLoader.class))
        .build();
    private final Set<ClassLoaderRemapper.WrappedMethod> defineClassMethods = defineClassTypes.entrySet().stream()
        .map(LamdbaExceptionUtils.rethrowFunction(entry -> {
            MethodType type = MethodType.fromMethodDescriptorString(entry.getKey().getDescriptor(), getClass().getClassLoader());
            return new ClassLoaderRemapper.WrappedMethod("defineClass", type.parameterArray());
        })).collect(Collectors.toSet());

    public static boolean isDefineClassMethod(Class<?> cl, String bukkitName, Class<?>[] pTypes) {
        if (bukkitName.equals("defineClass") && ClassLoader.class.isAssignableFrom(cl)) {
            return INSTANCE.defineClassMethods.contains(new ClassLoaderRemapper.WrappedMethod(bukkitName, pTypes));
        } else {
            return false;
        }
    }

    public static boolean isDefineClassMethod(Class<?> cl, String bukkitName, MethodType methodType) {
        if (bukkitName.equals("defineClass") && ClassLoader.class.isAssignableFrom(cl)) {
            return INSTANCE.defineClassMethods.contains(new ClassLoaderRemapper.WrappedMethod(bukkitName, methodType.parameterArray()));
        } else {
            return false;
        }
    }

    @Override
    public void handleClass(ClassNode node, ClassLoaderRemapper remapper) {
        for (MethodNode methodNode : node.methods) {
            for (AbstractInsnNode insnNode : methodNode.instructions) {
                if (insnNode.getOpcode() == Opcodes.NEW) {
                    TypeInsnNode typeInsnNode = (TypeInsnNode) insnNode;
                    String replace = classLoaderTypes.get(typeInsnNode.desc);
                    if (replace != null) {
                        AbstractInsnNode next = typeInsnNode.getNext();
                        while (next != null && (next.getOpcode() != Opcodes.INVOKESPECIAL || !((MethodInsnNode) next).name.equals("<init>") || !((MethodInsnNode) next).owner.equals(typeInsnNode.desc))) {
                            next = next.getNext();
                        }
                        if (next == null) continue;
                        ArclightMod.LOGGER.debug(MARKER, "Found new {}/{} call in {} {}", typeInsnNode.desc, ((MethodInsnNode) next).name + ((MethodInsnNode) next).desc, node.name, methodNode.name + methodNode.desc);
                        ((MethodInsnNode) next).owner = replace;
                        typeInsnNode.desc = replace;
                    }
                }
            }
        }
        ClassInfo info = classInfo(node);
        if (info == null) return;
        ArclightMod.LOGGER.debug(MARKER, "Transforming classloader class {}", node.name);
        if (!info.remapping) {
            implementIntf(node);
        }
        for (MethodNode methodNode : node.methods) {
            for (AbstractInsnNode insnNode : methodNode.instructions) {
                if (insnNode instanceof MethodInsnNode) {
                    MethodInsnNode methodInsnNode = (MethodInsnNode) insnNode;
                    if (methodInsnNode.getOpcode() == Opcodes.INVOKESPECIAL && methodNode.name.equals("<init>") && methodInsnNode.name.equals("<init>") && methodInsnNode.owner.equals(node.superName)) {
                        methodInsnNode.owner = info.superName;
                    }
                    if (methodInsnNode.name.equals("defineClass")) {
                        Type descType = Type.getMethodType(methodInsnNode.desc);
                        Map.Entry<Type, int[]> entry = defineClassTypes.get(descType);
                        if (entry != null && GlobalClassRepo.inheritanceProvider().getAll(methodInsnNode.owner).contains(entry.getKey().getInternalName())) {
                            ArclightMod.LOGGER.debug(MARKER, "Found defineClass {} call in {} {}", descType.getInternalName(), node.name, methodNode.name + methodNode.desc);
                            int index = entry.getValue()[0];
                            int lengthIndex = entry.getValue()[1];
                            InsnList insnList = new InsnList();
                            Type[] argumentTypes = descType.getArgumentTypes();
                            int[] argsMap = argsMap(argumentTypes);
                            storeArgs(argumentTypes, argsMap, methodNode, insnList);
                            insnList.add(new InsnNode(Opcodes.DUP));
                            insnList.add(new VarInsnNode(argumentTypes[index].getOpcode(Opcodes.ILOAD), methodNode.maxLocals + argsMap[index]));
                            insnList.add(new MethodInsnNode(Opcodes.INVOKESTATIC, Type.getInternalName(ClassLoaderAdapter.class), "remapClassContent", "(Ljava/lang/ClassLoader;Ljava/lang/Object;)Ljava/lang/Object;", false));
                            insnList.add(new TypeInsnNode(Opcodes.CHECKCAST, argumentTypes[index].getInternalName()));
                            if (lengthIndex != -1) {
                                insnList.add(new InsnNode(Opcodes.DUP));
                                insnList.add(new InsnNode(Opcodes.ARRAYLENGTH));
                                insnList.add(new VarInsnNode(argumentTypes[lengthIndex].getOpcode(Opcodes.ISTORE), methodNode.maxLocals + argsMap[lengthIndex]));
                            }
                            insnList.add(new VarInsnNode(argumentTypes[index].getOpcode(Opcodes.ISTORE), methodNode.maxLocals + argsMap[index]));
                            loadArgs(argumentTypes, argsMap, methodNode, insnList);
                            methodNode.instructions.insertBefore(methodInsnNode, insnList);
                        }
                    }
                }
            }
        }
        node.superName = info.superName;
    }

    @SuppressWarnings("unused")
    public static Object remapClassContent(ClassLoader classLoader, Object classContent) {
        ArclightMod.LOGGER.trace(MARKER, "Remapping class content {} from classloader {}", classContent, classLoader);
        if (!(classLoader instanceof RemappingClassLoader)) {
            throw new IllegalArgumentException("" + classLoader + " is not a remapping class loader!");
        }
        byte[] classBytes;
        if (classContent instanceof byte[]) {
            classBytes = ((byte[]) classContent);
        } else if (classContent instanceof ByteBuffer) {
            classBytes = new byte[((ByteBuffer) classContent).remaining()];
            ((ByteBuffer) classContent).get(classBytes);
        } else {
            throw new IllegalArgumentException("" + classContent + " is not a recognized class content type!");
        }
        byte[] bytes = ((RemappingClassLoader) classLoader).getRemapper().remapClass(classBytes);
        if (classContent instanceof byte[]) {
            return bytes;
        } else {
            return ByteBuffer.wrap(bytes);
        }
    }

    private static int[] argsMap(Type[] args) {
        int[] ints = new int[args.length];
        int offset = 0;
        for (int i = 0; i < args.length; i++) {
            Type arg = args[i];
            ints[i] = offset;
            offset += arg.getSize();
        }
        return ints;
    }

    private static void storeArgs(Type[] args, int[] argsMap, MethodNode node, InsnList list) {
        int start = node.maxLocals;
        for (int i = args.length - 1; i >= 0; i--) {
            Type arg = args[i];
            list.add(new VarInsnNode(arg.getOpcode(Opcodes.ISTORE), start + argsMap[i]));
        }
    }

    private static void loadArgs(Type[] args, int[] argsMap, MethodNode node, InsnList list) {
        int start = node.maxLocals;
        for (int i = 0; i < args.length; i++) {
            Type arg = args[i];
            list.add(new VarInsnNode(arg.getOpcode(Opcodes.ILOAD), start + argsMap[i]));
            node.maxLocals += argsMap[i];
        }
    }

    private void implementIntf(ClassNode node) {
        ArclightMod.LOGGER.debug(MARKER, "Implementing RemappingClassLoader for class {}", node.name);
        FieldNode remapper = new FieldNode(Opcodes.ACC_PRIVATE | Opcodes.ACC_SYNTHETIC, "remapper", Type.getDescriptor(ClassLoaderRemapper.class), null, null);
        MethodNode methodNode = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC, "getRemapper", Type.getMethodDescriptor(Type.getType(ClassLoaderRemapper.class)), null, null);
        InsnList list = new InsnList();
        LabelNode labelNode = new LabelNode();
        list.add(new VarInsnNode(Opcodes.ALOAD, 0));
        list.add(new FieldInsnNode(Opcodes.GETFIELD, node.name, remapper.name, remapper.desc));
        list.add(new JumpInsnNode(Opcodes.IFNONNULL, labelNode));
        list.add(new VarInsnNode(Opcodes.ALOAD, 0));
        list.add(new InsnNode(Opcodes.DUP));
        list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, Type.getInternalName(ArclightRemapper.class), "createClassLoaderRemapper", Type.getMethodDescriptor(Type.getType(ClassLoaderRemapper.class), Type.getType(ClassLoader.class)), false));
        list.add(new FieldInsnNode(Opcodes.PUTFIELD, node.name, remapper.name, remapper.desc));
        list.add(labelNode);
        list.add(new VarInsnNode(Opcodes.ALOAD, 0));
        list.add(new FieldInsnNode(Opcodes.GETFIELD, node.name, remapper.name, remapper.desc));
        list.add(new InsnNode(Opcodes.ARETURN));
        methodNode.instructions = list;
        node.fields.add(remapper);
        node.methods.add(methodNode);
        node.interfaces.add(Type.getInternalName(RemappingClassLoader.class));
    }

    private ClassInfo classInfo(ClassNode node) {
        ClassInfo info = new ClassInfo();
        Collection<String> parents = GlobalClassRepo.inheritanceProvider().getAll(node.superName);
        if (!parents.contains(CLASSLOADER)) return null;
        for (String s : classLoaderTypes.keySet()) {
            if (parents.contains(s)) {
                info.remapping = true;
                break;
            }
        }
        String s = classLoaderTypes.get(node.superName);
        if (s != null) {
            info.superName = s;
        } else {
            info.superName = node.superName;
        }
        return info;
    }

    private static class ClassInfo {

        private String superName;
        private boolean remapping;
    }
}
