package io.izzel.arclight.common.mod.util.remapper;

import com.google.common.collect.ImmutableMap;
import io.izzel.arclight.common.mod.ArclightMod;
import io.izzel.arclight.common.mod.util.remapper.generated.ArclightReflectionHandler;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;

public class ArclightRedirectAdapter implements PluginTransformer {

    public static final ArclightRedirectAdapter INSTANCE = new ArclightRedirectAdapter();
    private static final Marker MARKER = MarkerManager.getMarker("REDIRECT");
    private static final String REPLACED_NAME = Type.getInternalName(ArclightReflectionHandler.class);
    private static final Map<MethodInsnNode, MethodInsnNode> METHOD_REDIRECTS = ImmutableMap
        .<MethodInsnNode, MethodInsnNode>builder()
        .put(
            method(Opcodes.INVOKEVIRTUAL, Field.class, "getName"),
            method(Opcodes.INVOKESTATIC, ArclightReflectionHandler.class, "redirectFieldGetName", Field.class)
        )
        .put(
            method(Opcodes.INVOKEVIRTUAL, Class.class, "getField", String.class),
            method(Opcodes.INVOKESTATIC, ArclightReflectionHandler.class, "redirectGetField", Class.class, String.class)
        )
        .put(
            method(Opcodes.INVOKEVIRTUAL, Class.class, "getDeclaredField", String.class),
            method(Opcodes.INVOKESTATIC, ArclightReflectionHandler.class, "redirectGetDeclaredField", Class.class, String.class)
        )
        .put(
            method(Opcodes.INVOKEVIRTUAL, Class.class, "getName"),
            method(Opcodes.INVOKESTATIC, ArclightReflectionHandler.class, "redirectClassGetName", Class.class)
        )
        .put(
            method(Opcodes.INVOKEVIRTUAL, Class.class, "getCanonicalName"),
            method(Opcodes.INVOKESTATIC, ArclightReflectionHandler.class, "redirectClassGetCanonicalName", Class.class)
        )
        .put(
            method(Opcodes.INVOKEVIRTUAL, Class.class, "getSimpleName"),
            method(Opcodes.INVOKESTATIC, ArclightReflectionHandler.class, "redirectClassGetSimpleName", Class.class)
        )
        .put(
            method(Opcodes.INVOKEVIRTUAL, Method.class, "getName"),
            method(Opcodes.INVOKESTATIC, ArclightReflectionHandler.class, "redirectMethodGetName", Method.class)
        )
        .put(
            method(Opcodes.INVOKEVIRTUAL, Class.class, "getMethod", String.class, Class[].class),
            method(Opcodes.INVOKESTATIC, ArclightReflectionHandler.class, "redirectGetMethod", Class.class, String.class, Class[].class)
        )
        .put(
            method(Opcodes.INVOKEVIRTUAL, Class.class, "getDeclaredMethod", String.class, Class[].class),
            method(Opcodes.INVOKESTATIC, ArclightReflectionHandler.class, "redirectGetDeclaredMethod", Class.class, String.class, Class[].class)
        )
        .put(
            method(Opcodes.INVOKESTATIC, Class.class, "forName", String.class),
            method(Opcodes.INVOKESTATIC, ArclightReflectionHandler.class, "redirectForName", String.class)
        )
        .put(
            method(Opcodes.INVOKESTATIC, Class.class, "forName", String.class, boolean.class, ClassLoader.class),
            method(Opcodes.INVOKESTATIC, ArclightReflectionHandler.class, "redirectForName", String.class, boolean.class, ClassLoader.class)
        )
        .put(
            method(Opcodes.INVOKEVIRTUAL, Package.class, "getName"),
            method(Opcodes.INVOKESTATIC, ArclightReflectionHandler.class, "redirectPackageGetName", Package.class)
        )
        .put(
            method(Opcodes.INVOKESTATIC, MethodType.class, "fromMethodDescriptorString", String.class, ClassLoader.class),
            method(Opcodes.INVOKESTATIC, ArclightReflectionHandler.class, "redirectFromDescStr", String.class, ClassLoader.class)
        )
        .put(
            method(Opcodes.INVOKEVIRTUAL, MethodHandles.Lookup.class, "findStatic", Class.class, String.class, MethodType.class),
            method(Opcodes.INVOKESTATIC, ArclightReflectionHandler.class, "redirectFindStatic", MethodHandles.Lookup.class, Class.class, String.class, MethodType.class)
        )
        .put(
            method(Opcodes.INVOKEVIRTUAL, MethodHandles.Lookup.class, "findVirtual", Class.class, String.class, MethodType.class),
            method(Opcodes.INVOKESTATIC, ArclightReflectionHandler.class, "redirectFindVirtual", MethodHandles.Lookup.class, Class.class, String.class, MethodType.class)
        )
        .put(
            method(Opcodes.INVOKEVIRTUAL, MethodHandles.Lookup.class, "findSpecial", Class.class, String.class, MethodType.class, Class.class),
            method(Opcodes.INVOKESTATIC, ArclightReflectionHandler.class, "redirectFindSpecial", MethodHandles.Lookup.class, Class.class, String.class, MethodType.class, Class.class)
        )
        .put(
            method(Opcodes.INVOKEVIRTUAL, MethodHandles.Lookup.class, "findGetter", Class.class, String.class, Class.class),
            method(Opcodes.INVOKESTATIC, ArclightReflectionHandler.class, "redirectFindGetter", MethodHandles.Lookup.class, Class.class, String.class, Class.class)
        )
        .put(
            method(Opcodes.INVOKEVIRTUAL, MethodHandles.Lookup.class, "findSetter", Class.class, String.class, Class.class),
            method(Opcodes.INVOKESTATIC, ArclightReflectionHandler.class, "redirectFindSetter", MethodHandles.Lookup.class, Class.class, String.class, Class.class)
        )
        .put(
            method(Opcodes.INVOKEVIRTUAL, MethodHandles.Lookup.class, "findStaticGetter", Class.class, String.class, Class.class),
            method(Opcodes.INVOKESTATIC, ArclightReflectionHandler.class, "redirectFindStaticGetter", MethodHandles.Lookup.class, Class.class, String.class, Class.class)
        )
        .put(
            method(Opcodes.INVOKEVIRTUAL, MethodHandles.Lookup.class, "findStaticSetter", Class.class, String.class, Class.class),
            method(Opcodes.INVOKESTATIC, ArclightReflectionHandler.class, "redirectFindStaticSetter", MethodHandles.Lookup.class, Class.class, String.class, Class.class)
        )
        .put(
            method(Opcodes.INVOKEVIRTUAL, ClassLoader.class, "loadClass", String.class),
            method(Opcodes.INVOKESTATIC, ArclightReflectionHandler.class, "redirectClassLoaderLoadClass", ClassLoader.class, String.class)
        )
        .build();
    private static final String METHOD_SIG = Type.getInternalName(Method.class);
    private static final String INVOKE_SIG = Type.getMethodDescriptor(Type.getType(Object.class), Type.getType(Object.class), Type.getType(Object[].class));

    @Override
    public void handleClass(ClassNode node, ClassLoaderRemapper remapper) {
        redirect(node, remapper.getGeneratedHandler());
    }

    private static void redirect(ClassNode classNode, String generatedOwner) {
        boolean defineClassFound = false;
        for (MethodNode methodNode : classNode.methods) {
            ListIterator<AbstractInsnNode> iterator = methodNode.instructions.iterator();
            while (iterator.hasNext()) {
                AbstractInsnNode insnNode = iterator.next();
                if (insnNode instanceof MethodInsnNode) {
                    MethodInsnNode from = (MethodInsnNode) insnNode;
                    for (Map.Entry<MethodInsnNode, MethodInsnNode> entry : METHOD_REDIRECTS.entrySet()) {
                        MethodInsnNode key = entry.getKey();
                        if (
                            key.getOpcode() == from.getOpcode() &&
                                Objects.equals(key.owner, from.owner) &&
                                Objects.equals(key.name, from.name) &&
                                Objects.equals(key.desc, from.desc)) {
                            MethodInsnNode to = entry.getValue();
                            if (REPLACED_NAME.equals(to.owner)) {
                                MethodInsnNode clone = (MethodInsnNode) to.clone(ImmutableMap.of());
                                clone.owner = generatedOwner;
                                iterator.set(clone);
                            } else {
                                iterator.set(to);
                            }
                        }
                    }
                } else if (insnNode.getOpcode() == Opcodes.LDC) {
                    defineClassFound |= "defineClass".equals(((LdcInsnNode) insnNode).cst);
                }
            }
        }
        if (defineClassFound) {
            for (MethodNode methodNode : classNode.methods) {
                for (AbstractInsnNode insnNode : methodNode.instructions) {
                    if (insnNode.getOpcode() == Opcodes.INVOKEVIRTUAL) {
                        MethodInsnNode methodInsnNode = (MethodInsnNode) insnNode;
                        if (methodInsnNode.owner.equals(METHOD_SIG) && methodInsnNode.name.equals("invoke") && methodInsnNode.desc.equals(INVOKE_SIG)) {
                            methodInsnNode.setOpcode(Opcodes.INVOKESTATIC);
                            methodInsnNode.owner = generatedOwner;
                            methodInsnNode.name = "redirectDefineClassInvoke";
                            methodInsnNode.desc = "(L" + METHOD_SIG + ";" + methodInsnNode.desc.substring(1);
                            ArclightMod.LOGGER.debug(MARKER, "Redirect candidate defineClass method invoke in {}/{} {}"
                                , classNode.name, methodNode.name, methodNode.desc);
                        }
                    }
                }
            }
        }
    }

    private static MethodInsnNode method(int opcode, Class<?> cl, String name, Class<?>... pTypes) {
        try {
            return method(opcode, cl.getMethod(name, pTypes));
        } catch (Exception e) {
            try {
                return method(opcode, cl.getDeclaredMethod(name, pTypes));
            } catch (NoSuchMethodException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    private static MethodInsnNode method(int opcode, Method method) {
        String owner = Type.getInternalName(method.getDeclaringClass());
        String name = method.getName();
        String desc = Type.getMethodDescriptor(method);
        return new MethodInsnNode(opcode, owner, name, desc);
    }
}
