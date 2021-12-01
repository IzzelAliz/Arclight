package io.izzel.arclight.common.mod.util.remapper;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import io.izzel.arclight.api.Unsafe;
import io.izzel.arclight.common.mod.ArclightMod;
import io.izzel.arclight.common.mod.util.remapper.generated.ArclightReflectionHandler;
import io.izzel.arclight.common.util.ArrayUtil;
import io.izzel.tools.func.Func4;
import io.izzel.tools.product.Product;
import io.izzel.tools.product.Product2;
import org.apache.commons.lang3.ClassUtils;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.objectweb.asm.ClassReader;
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
import java.util.Arrays;
import java.util.Collection;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class ArclightRedirectAdapter implements PluginTransformer {

    public static final ArclightRedirectAdapter INSTANCE = new ArclightRedirectAdapter();
    private static final Marker MARKER = MarkerManager.getMarker("REDIRECT");
    private static final String REPLACED_NAME = Type.getInternalName(ArclightReflectionHandler.class);
    private static final Multimap<String, Product2<String, MethodInsnNode>> METHOD_MODIFY = HashMultimap.create();
    private static final Multimap<String, Product2<String, MethodInsnNode>> METHOD_REDIRECT = HashMultimap.create();
    private static final Map<String, Func4<ClassLoaderRemapper, Method, Object, Object[], Object[]>> METHOD_TO_HANDLER = new ConcurrentHashMap<>();

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
        redirect(Class.class, "getDeclaredMethods", "getDeclaredMethods");
        redirect(Class.class, "getMethods", "getMethods");
        redirect(Class.class, "getDeclaredFields", "getDeclaredFields");
        redirect(Class.class, "getFields", "getFields");
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
        modify(classOf("sun.misc.Unsafe"), "defineClass", "unsafeDefineClass", String.class, byte[].class, int.class, int.class, ClassLoader.class, ProtectionDomain.class);
        modify(classOf("jdk.internal.misc.Unsafe"), "defineClass", "unsafeDefineClass", String.class, byte[].class, int.class, int.class, ClassLoader.class, ProtectionDomain.class);
        modify(classOf("jdk.internal.misc.Unsafe"), "defineClass0", "unsafeDefineClass", String.class, byte[].class, int.class, int.class, ClassLoader.class, ProtectionDomain.class);
        modify(MethodHandles.Lookup.class, "defineClass", "lookupDefineClass", byte[].class);
    }

    public static Object[] runHandle(ClassLoaderRemapper remapper, Method method, Object src, Object[] param) {
        Func4<ClassLoaderRemapper, Method, Object, Object[], Object[]> handler = METHOD_TO_HANDLER.get(methodToString(method));
        if (handler != null) {
            return handler.apply(remapper, method, src, param);
        }
        return null;
    }

    public static Object runRedirect(ClassLoaderRemapper remapper, Method method, Object src, Object[] param) throws Throwable {
        Func4<ClassLoaderRemapper, Method, Object, Object[], Object[]> handler = METHOD_TO_HANDLER.get(methodToString(method));
        if (handler != null) {
            Object[] ret = handler.apply(remapper, method, src, param);
            return ((Method) ret[0]).invoke(ret[1], (Object[]) ret[2]);
        }
        return remapper;
    }

    public static void scanMethod(byte[] bytes) {
        ClassReader reader = new ClassReader(bytes);
        ArclightMod.LOGGER.debug(MARKER, "Scanning {}", reader.getClassName());
        ClassNode node = new ClassNode();
        reader.accept(node, ClassReader.SKIP_FRAMES | ClassReader.SKIP_DEBUG);
        for (MethodNode method : node.methods) {
            for (ListIterator<AbstractInsnNode> iterator = method.instructions.iterator(); iterator.hasNext(); ) {
                AbstractInsnNode instruction = iterator.next();
                int opcode = instruction.getOpcode();
                if (opcode >= Opcodes.INVOKEVIRTUAL && opcode <= Opcodes.INVOKEINTERFACE) {
                    if (iterator.nextIndex() < method.instructions.size() - 1) {
                        break;
                    }
                    MethodInsnNode insnNode = (MethodInsnNode) instruction;
                    String key = insnNode.name + insnNode.desc;
                    if (METHOD_MODIFY.containsKey(key) || METHOD_REDIRECT.containsKey(key)) {
                        try {
                            Class<?> cl = Class.forName(insnNode.owner.replace('/', '.'));
                            Type[] argumentTypes = Type.getMethodType(insnNode.desc).getArgumentTypes();
                            Class<?>[] paramTypes = new Class<?>[argumentTypes.length];
                            for (int i = 0, argumentTypesLength = argumentTypes.length; i < argumentTypesLength; i++) {
                                Type type = argumentTypes[i];
                                paramTypes[i] = ClassUtils.getClass(type.getClassName());
                            }
                            Method target = methodOf(cl, insnNode.name, paramTypes);
                            if (target != null) {
                                Func4<ClassLoaderRemapper, Method, Object, Object[], Object[]> bridge = METHOD_TO_HANDLER.get(methodToString(target));
                                if (bridge != null) {
                                    ArclightMod.LOGGER.debug(MARKER, "Creating bridge handler {}/{}{} to {}", node.name, method.name, method.desc, methodToString(target));
                                    METHOD_TO_HANDLER.put(node.name + '/' + method.name + method.desc, new BridgeHandler(bridge, target));
                                }
                            }
                        } catch (ClassNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                } else if (opcode < Opcodes.ILOAD || opcode > Opcodes.ALOAD) {
                    break;
                }
            }
        }
    }

    @Override
    public void handleClass(ClassNode node, ClassLoaderRemapper remapper) {
        redirect(node, remapper);
    }

    private static void redirect(ClassNode classNode, ClassLoaderRemapper remapper) {
        for (MethodNode methodNode : classNode.methods) {
            for (AbstractInsnNode insnNode : methodNode.instructions) {
                if (insnNode instanceof MethodInsnNode from) {
                    if (from.getOpcode() == Opcodes.INVOKESPECIAL
                        && Objects.equals(from.owner, classNode.superName)
                        && Objects.equals(from.name, methodNode.name)
                        && Objects.equals(from.desc, methodNode.desc)) {
                        continue;
                    }
                    process(from, methodNode.instructions, remapper, classNode);
                } else if (insnNode.getOpcode() == Opcodes.INVOKEDYNAMIC) {
                    InvokeDynamicInsnNode invokeDynamic = (InvokeDynamicInsnNode) insnNode;
                    Object[] bsmArgs = invokeDynamic.bsmArgs;
                    for (int i = 0; i < bsmArgs.length; i++) {
                        Object bsmArg = bsmArgs[i];
                        if (bsmArg instanceof Handle handle) {
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

    private static void process(MethodInsnNode node, InsnList insnList, ClassLoaderRemapper remapper, ClassNode classNode) {
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
                processModify(node, insnList, handlerNode, classNode);
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

    private static void processModify(MethodInsnNode node, InsnList insnList, MethodInsnNode handlerNode, ClassNode classNode) {
        InsnList list = new InsnList();
        list.add(handlerNode);
        Type methodType = Type.getMethodType(node.desc);
        Type[] types = methodType.getArgumentTypes();
        if (node.getOpcode() != Opcodes.INVOKESTATIC) {
            Type selfType;
            if (node.getOpcode() == Opcodes.INVOKESPECIAL) {
                selfType = Type.getObjectType(classNode.name);
            } else {
                selfType = Type.getObjectType(node.owner);
            }
            types = ArrayUtil.prepend(types, selfType, Type[]::new);
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
        if (owner == null) return;
        Method original = methodOf(owner, name, args);
        if (original == null) return;
        Class<?>[] handlerArgs;
        if (!Modifier.isStatic(original.getModifiers())) {
            handlerArgs = ArrayUtil.prepend(args, owner, Class[]::new);
        } else {
            handlerArgs = args;
        }
        Method handler = methodOf(ArclightReflectionHandler.class, "redirect" + capitalize(handlerName), handlerArgs);
        while (handler == null) {
            handlerArgs[0] = handlerArgs[0].getSuperclass();
            handler = methodOf(ArclightReflectionHandler.class, "redirect" + capitalize(handlerName), handlerArgs);
        }
        METHOD_REDIRECT.put(name + Type.getMethodDescriptor(original), Product.of(Type.getInternalName(owner), methodNodeOf(handler)));
        String key = methodToString(original);
        if (modifyArgs) {
            Method modifyHandler = methodOf(ArclightReflectionHandler.class, "handle" + capitalize(handlerName), handlerArgs);
            if (modifyHandler == null) {
                handlerArgs[0] = original.getReturnType();
                modifyHandler = methodOf(ArclightReflectionHandler.class, "handle" + capitalize(handlerName), handlerArgs);
            }
            if (modifyHandler == null) {
                throw new RuntimeException("No handler for " + original);
            }
            METHOD_MODIFY.put(name + Type.getMethodDescriptor(original), Product.of(Type.getInternalName(owner), methodNodeOf(modifyHandler)));
            METHOD_TO_HANDLER.put(key, new ModifyHandler("handle" + capitalize(handlerName), handlerArgs));
        } else {
            METHOD_TO_HANDLER.put(key, new RedirectHandler("redirect" + capitalize(handlerName), handlerArgs));
        }
    }

    private static String capitalize(String name) {
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    private static Class<?> classOf(String cl) {
        try {
            return Class.forName(cl);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    private static Method methodOf(Class<?> owner, String name, Class<?>... args) {
        try {
            return owner.getMethod(name, args);
        } catch (Exception e) {
            try {
                return owner.getDeclaredMethod(name, args);
            } catch (NoSuchMethodException e2) {
                return null;
            }
        }
    }

    private static MethodInsnNode methodNodeOf(Method method) {
        String owner = Type.getInternalName(method.getDeclaringClass());
        String name = method.getName();
        String desc = Type.getMethodDescriptor(method);
        return new MethodInsnNode(Opcodes.INVOKESTATIC, owner, name, desc);
    }

    private static String methodToString(Method method) {
        return Type.getInternalName(method.getDeclaringClass()) + "/" + method.getName() + Type.getMethodDescriptor(method);
    }

    private static int toOpcode(int handleType) {
        return switch (handleType) {
            case Opcodes.H_INVOKEINTERFACE -> Opcodes.INVOKEINTERFACE;
            case Opcodes.H_INVOKEVIRTUAL -> Opcodes.INVOKEVIRTUAL;
            case Opcodes.H_INVOKESTATIC -> Opcodes.INVOKESTATIC;
            default -> -1;
        };
    }

    private static int toHandle(int opcode) {
        return switch (opcode) {
            case Opcodes.INVOKEINTERFACE -> Opcodes.H_INVOKEINTERFACE;
            case Opcodes.INVOKESTATIC -> Opcodes.H_INVOKESTATIC;
            case Opcodes.INVOKEVIRTUAL -> Opcodes.H_INVOKEVIRTUAL;
            default -> -1;
        };
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

    private static class ModifyHandler implements Func4<ClassLoaderRemapper, Method, Object, Object[], Object[]> {

        private final String handlerName;
        private final Class<?>[] handlerArgs;

        public ModifyHandler(String handlerName, Class<?>[] handlerArgs) {
            this.handlerName = handlerName;
            this.handlerArgs = handlerArgs;
        }

        @Override
        public Object[] apply4(ClassLoaderRemapper remapper, Method method, Object src, Object[] param) {
            try {
                Method handleMethod = remapper.getGeneratedHandlerClass().getMethod(handlerName, handlerArgs);
                if (method.getParameterCount() > 0) {
                    if (handleMethod.getReturnType().isArray() && !Modifier.isStatic(method.getModifiers())) {
                        Object[] invoke = (Object[]) handleMethod.invoke(null, ArrayUtil.prepend(param, src));
                        return new Object[]{method, invoke[0], Arrays.copyOfRange(invoke, 1, invoke.length)};
                    } else {
                        return new Object[]{method, src, handleMethod.invoke(null, param)};
                    }
                } else {
                    return new Object[]{handleMethod, null, new Object[]{method.invoke(src, param)}};
                }
            } catch (Exception e) {
                Unsafe.throwException(e);
            }
            return null;
        }
    }

    private static class RedirectHandler implements Func4<ClassLoaderRemapper, Method, Object, Object[], Object[]> {

        private final String handlerName;
        private final Class<?>[] handlerArgs;

        public RedirectHandler(String handlerName, Class<?>[] handlerArgs) {
            this.handlerName = handlerName;
            this.handlerArgs = handlerArgs;
        }

        @Override
        public Object[] apply4(ClassLoaderRemapper remapper, Method method, Object src, Object[] param) {
            try {
                Method redirectMethod = remapper.getGeneratedHandlerClass().getMethod(handlerName, handlerArgs);
                return new Object[]{redirectMethod, null, Modifier.isStatic(method.getModifiers()) ? param : ArrayUtil.prepend(param, src)};
            } catch (Exception e) {
                Unsafe.throwException(e);
                return null;
            }
        }
    }

    private static class BridgeHandler implements Func4<ClassLoaderRemapper, Method, Object, Object[], Object[]> {

        private final Func4<ClassLoaderRemapper, Method, Object, Object[], Object[]> bridge;
        private final Method targetMethod;

        private BridgeHandler(Func4<ClassLoaderRemapper, Method, Object, Object[], Object[]> bridge, Method targetMethod) {
            this.bridge = bridge;
            this.targetMethod = targetMethod;
        }

        @Override
        public Object[] apply4(ClassLoaderRemapper remapper, Method method, Object src, Object[] param) {
            boolean bridgeStatic = Modifier.isStatic(targetMethod.getModifiers());
            if (bridgeStatic) {
                Object[] ret = bridge.apply(remapper, this.targetMethod, null, param);
                return new Object[]{method, src, ret[2]};
            } else {
                Object[] ret = bridge.apply(remapper, this.targetMethod, param[0], Arrays.copyOfRange(param, 1, param.length));
                return new Object[]{method, src, ArrayUtil.prepend((Object[]) ret[2], ret[1])};
            }
        }
    }
}
