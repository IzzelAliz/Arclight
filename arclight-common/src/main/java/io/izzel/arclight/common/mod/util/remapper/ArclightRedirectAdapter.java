package io.izzel.arclight.common.mod.util.remapper;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import io.izzel.arclight.common.mod.util.remapper.generated.ArclightReflectionHandler;
import io.izzel.arclight.common.util.ArrayUtil;
import io.izzel.tools.product.Product;
import io.izzel.tools.product.Product2;
import io.izzel.tools.product.Product4;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.spongepowered.asm.util.Bytecode;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.security.SecureClassLoader;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ArclightRedirectAdapter implements PluginTransformer {

    public static final ArclightRedirectAdapter INSTANCE = new ArclightRedirectAdapter();
    private static final Marker MARKER = MarkerManager.getMarker("REDIRECT");
    private static final String REPLACED_NAME = Type.getInternalName(ArclightReflectionHandler.class);
    private static final Multimap<String, Product2<String, MethodInsnNode>> METHOD_MODIFY = HashMultimap.create();
    private static final Multimap<String, Product2<String, MethodInsnNode>> METHOD_REDIRECT = HashMultimap.create();
    private static final Map<Method, Product4<String, Class<?>[], String, Class<?>[]>> METHOD_TO_HANDLER = new HashMap<>();

    static {
        redirect(Field.class, "getName", "fieldGetName");
        redirect(Method.class, "getName", "methodGetName");
        redirect(Class.class, "getCanonicalName", "classGetCanonicalName");
        redirect(Class.class, "getSimpleName", "classGetSimpleName");
        modify(Class.class, "getName", "classGetName");
        modify(Package.class, "getName", "packageGetName");
        redirect(Class.class, "forName", "classForName", String.class);
        redirect(Class.class, "forName", "classForName", String.class, boolean.class, ClassLoader.class);
        modify(Class.class, "getField", "classGetField", String.class);
        modify(Class.class, "getDeclaredField", "classGetDeclaredField", String.class);
        modify(Class.class, "getMethod", "classGetMethod", String.class, Class[].class);
        modify(Class.class, "getDeclaredMethod", "classGetDeclaredMethod", String.class, Class[].class);
        modify(MethodType.class, "fromMethodDescriptorString", "fromDescStr", String.class, ClassLoader.class);
        modify(MethodHandles.Lookup.class, "findStatic", "lookupFindStatic", Class.class, String.class, MethodType.class);
        modify(MethodHandles.Lookup.class, "findVirtual", "lookupFindVirtual", Class.class, String.class, MethodType.class);
        modify(MethodHandles.Lookup.class, "findSpecial", "lookupFindSpecial", Class.class, String.class, MethodType.class, Class.class);
        modify(MethodHandles.Lookup.class, "findGetter", "lookupFindGetter", Class.class, String.class, Class.class);
        modify(MethodHandles.Lookup.class, "findSetter", "lookupFindSetter", Class.class, String.class, Class.class);
        modify(MethodHandles.Lookup.class, "findStaticGetter", "lookupFindStaticGetter", Class.class, String.class, Class.class);
        modify(MethodHandles.Lookup.class, "findStaticSetter", "lookupFindStaticSetter", Class.class, String.class, Class.class);
        modify(ClassLoader.class, "loadClass", "classLoaderLoadClass", String.class);
        redirect(Class.class, "getResource", "classGetResource", String.class);
        redirect(Class.class, "getResourceAsStream", "classGetResourceAsStream", String.class);
        redirect(ClassLoader.class, "getResource", "classLoaderGetResource", String.class);
        redirect(ClassLoader.class, "getResources", "classLoaderGetResources", String.class);
        redirect(ClassLoader.class, "getResourceAsStream", "classLoaderGetResourceAsStream", String.class);
        modify(Method.class, "invoke", "methodInvoke", Object.class, Object[].class);
        modify(ClassLoader.class, "defineClass", byte[].class, int.class, int.class);
        modify(ClassLoader.class, "defineClass", String.class, byte[].class, int.class, int.class);
        modify(ClassLoader.class, "defineClass", String.class, byte[].class, int.class, int.class, ProtectionDomain.class);
        modify(ClassLoader.class, "defineClass", String.class, ByteBuffer.class, ProtectionDomain.class);
        modify(SecureClassLoader.class, "defineClass", String.class, byte[].class, int.class, int.class, CodeSource.class);
        modify(SecureClassLoader.class, "defineClass", String.class, ByteBuffer.class, CodeSource.class);
    }

    public static Product4<String, Class<?>[], String, Class<?>[]> getInvokeRule(Method method) {
        return METHOD_TO_HANDLER.get(method);
    }

    @Override
    public void handleClass(ClassNode node, ClassLoaderRemapper remapper) {
        redirect(node, remapper);
    }

    private static void redirect(ClassNode classNode, ClassLoaderRemapper remapper) {
        for (MethodNode methodNode : classNode.methods) {
            for (AbstractInsnNode insnNode : methodNode.instructions) {
                if (insnNode instanceof MethodInsnNode) {
                    MethodInsnNode from = (MethodInsnNode) insnNode;
                    process(from, methodNode.instructions, remapper);
                } else if (insnNode.getOpcode() == Opcodes.INVOKEDYNAMIC) {
                    InvokeDynamicInsnNode invokeDynamic = (InvokeDynamicInsnNode) insnNode;
                    Object[] bsmArgs = invokeDynamic.bsmArgs;
                    for (int i = 0; i < bsmArgs.length; i++) {
                        Object bsmArg = bsmArgs[i];
                        if (bsmArg instanceof Handle) {
                            Handle handle = (Handle) bsmArg;
                            if (toOpcode(handle.getTag()) != -1) {
                                bsmArgs[i] = processHandle(handle, remapper);
                            }
                        }
                    }
                }
            }
        }
    }

    private static Handle processHandle(Handle handle, ClassLoaderRemapper remapper) {
        String key = handle.getName() + handle.getDesc();
        Collection<Product2<String, MethodInsnNode>> col = METHOD_REDIRECT.get(key);
        for (Product2<String, MethodInsnNode> methodRedirect : col) {
            if (isSuperType(handle.getOwner(), methodRedirect._1)) {
                MethodInsnNode node = methodRedirect._2;
                String owner = REPLACED_NAME.equals(node.owner) ? remapper.getGeneratedHandler() : node.owner;
                return new Handle(toHandle(node.getOpcode()), owner, node.name, node.desc, node.itf);
            }
        }
        return handle;
    }

    private static void process(MethodInsnNode node, InsnList insnList, ClassLoaderRemapper remapper) {
        String key = node.name + node.desc;
        Collection<Product2<String, MethodInsnNode>> modifyArgsCol = METHOD_MODIFY.get(key);
        for (Product2<String, MethodInsnNode> modifyArgs : modifyArgsCol) {
            if (isSuperType(node.owner, modifyArgs._1)) {
                MethodInsnNode handlerNode;
                if (REPLACED_NAME.equals(modifyArgs._2.owner)) {
                    handlerNode = (MethodInsnNode) modifyArgs._2.clone(ImmutableMap.of());
                    handlerNode.owner = remapper.getGeneratedHandler();
                } else {
                    handlerNode = modifyArgs._2;
                }
                processModify(node, insnList, handlerNode);
                return;
            }
        }
        Collection<Product2<String, MethodInsnNode>> methodRedirectCol = METHOD_REDIRECT.get(key);
        for (Product2<String, MethodInsnNode> methodRedirect : methodRedirectCol) {
            if (isSuperType(node.owner, methodRedirect._1)) {
                MethodInsnNode handlerNode;
                if (REPLACED_NAME.equals(methodRedirect._2.owner)) {
                    handlerNode = (MethodInsnNode) methodRedirect._2.clone(ImmutableMap.of());
                    handlerNode.owner = remapper.getGeneratedHandler();
                } else {
                    handlerNode = methodRedirect._2;
                }
                processMethodRedirect(node, insnList, handlerNode);
                return;
            }
        }
    }

    private static boolean isSuperType(String sub, String sup) {
        return sub.equals(sup) || GlobalClassRepo.inheritanceProvider().getAll(sub).contains(sup);
    }

    private static void processMethodRedirect(MethodInsnNode node, InsnList insnList, MethodInsnNode handlerNode) {
        insnList.set(node, handlerNode);
    }

    private static void processModify(MethodInsnNode node, InsnList insnList, MethodInsnNode handlerNode) {
        InsnList list = new InsnList();
        list.add(handlerNode);
        Type methodType = Type.getMethodType(node.desc);
        Type[] types = methodType.getArgumentTypes();
        if (node.getOpcode() != Opcodes.INVOKESTATIC) {
            types = ArrayUtil.prepend(types, Type.getObjectType(node.owner), Type[]::new);
        }
        if (types.length == 1) {
            if (node.desc.startsWith("()")) {
                String retDesc = methodType.getReturnType().getDescriptor();
                if (handlerNode.desc.equals("(" + retDesc + ")" + retDesc)) { // handle(obj.method())
                    insnList.insert(node, handlerNode);
                    return;
                }
            } else {
                String desc = types[0].getDescriptor();
                if (handlerNode.desc.equals("(" + desc + ")" + desc)) { // object.call(handle(arg0))
                    insnList.insertBefore(node, handlerNode);
                    return;
                }
            }
        }
        for (int i = 0, argumentTypesLength = types.length; i < argumentTypesLength; i++) {
            Type type = types[i];
            if (i > 0) {
                swap(list, types[i - 1]);
            }
            if (argumentTypesLength > 1 && i != argumentTypesLength - 1) {
                list.add(new InsnNode(Opcodes.DUP));
            }
            list.add(loadInt(i));
            list.add(new InsnNode(Opcodes.AALOAD));
            cast(list, type);
        }
        insnList.insertBefore(node, list);
    }

    private static void swap(InsnList list, Type top) {
        if (top.getSize() == 1) {
            list.add(new InsnNode(Opcodes.SWAP));
        } else {
            list.add(new InsnNode(Opcodes.DUP2_X1));
            list.add(new InsnNode(Opcodes.POP2));
        }
    }

    private static void cast(InsnList list, Type type) {
        if (type.getSort() == Type.OBJECT || type.getSort() == Type.ARRAY) {
            String internalName = type.getInternalName();
            if (!"java/lang/Object".equals(internalName)) {
                list.add(new TypeInsnNode(Opcodes.CHECKCAST, internalName));
            }
        } else {
            String boxingType = Bytecode.getBoxingType(type);
            String unboxingMethod = Bytecode.getUnboxingMethod(type);
            list.add(new TypeInsnNode(Opcodes.CHECKCAST, boxingType));
            list.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, boxingType, unboxingMethod, "()" + type.getDescriptor(), false));
        }
    }

    private static void modify(Class<?> owner, String name, Class<?>... args) {
        modify(owner, name, name, args);
    }

    private static void modify(Class<?> owner, String name, String handlerName, Class<?>... args) {
        addRule(true, owner, name, handlerName, args);
    }

    private static void redirect(Class<?> owner, String name, String handlerName, Class<?>... args) {
        addRule(false, owner, name, handlerName, args);
    }

    private static void addRule(boolean modifyArgs, Class<?> owner, String name, String handlerName, Class<?>... args) {
        Method original = methodOf(owner, name, args);
        Class<?>[] handlerArgs;
        if (!Modifier.isStatic(original.getModifiers())) {
            handlerArgs = ArrayUtil.prepend(args, owner, Class[]::new);
        } else {
            handlerArgs = args;
        }
        Method handler = methodOf(ArclightReflectionHandler.class, "redirect" + capitalize(handlerName), handlerArgs);
        METHOD_REDIRECT.put(name + Type.getMethodDescriptor(original), Product.of(Type.getInternalName(owner), methodNodeOf(handler)));
        Product2<String, Class<?>[]> handleProd;
        if (modifyArgs) {
            Method modifyHandler;
            try {
                modifyHandler = methodOf(ArclightReflectionHandler.class, "handle" + capitalize(handlerName), handlerArgs);
            } catch (RuntimeException e) {
                handlerArgs[0] = original.getReturnType();
                modifyHandler = methodOf(ArclightReflectionHandler.class, "handle" + capitalize(handlerName), handlerArgs);
            }
            METHOD_MODIFY.put(name + Type.getMethodDescriptor(original), Product.of(Type.getInternalName(owner), methodNodeOf(modifyHandler)));
            handleProd = Product.of("handle" + capitalize(handlerName), handlerArgs);
        } else {
            handleProd = Product.of(null, null);
        }
        METHOD_TO_HANDLER.put(original, Product.of("redirect" + capitalize(handlerName), handlerArgs, handleProd._1, handleProd._2));
    }

    private static String capitalize(String name) {
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    private static Method methodOf(Class<?> owner, String name, Class<?>... args) {
        try {
            return owner.getMethod(name, args);
        } catch (Exception e) {
            try {
                return owner.getDeclaredMethod(name, args);
            } catch (NoSuchMethodException e2) {
                throw new RuntimeException(e2);
            }
        }
    }

    private static MethodInsnNode methodNodeOf(Method method) {
        String owner = Type.getInternalName(method.getDeclaringClass());
        String name = method.getName();
        String desc = Type.getMethodDescriptor(method);
        return new MethodInsnNode(Opcodes.INVOKESTATIC, owner, name, desc);
    }

    private static int toOpcode(int handleType) {
        switch (handleType) {
            case Opcodes.H_INVOKEINTERFACE:
                return Opcodes.INVOKEINTERFACE;
            case Opcodes.H_INVOKEVIRTUAL:
                return Opcodes.INVOKEVIRTUAL;
            case Opcodes.H_INVOKESTATIC:
                return Opcodes.INVOKESTATIC;
            default:
                return -1;
        }
    }

    private static int toHandle(int opcode) {
        switch (opcode) {
            case Opcodes.INVOKEINTERFACE:
                return Opcodes.H_INVOKEINTERFACE;
            case Opcodes.INVOKESTATIC:
                return Opcodes.H_INVOKESTATIC;
            case Opcodes.INVOKEVIRTUAL:
                return Opcodes.H_INVOKEVIRTUAL;
            default:
                return -1;
        }
    }

    static AbstractInsnNode loadInt(int i) {
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
}
