package io.izzel.arclight.common.mod.util.remapper.generated;

import io.izzel.arclight.api.ArclightVersion;
import io.izzel.arclight.api.Unsafe;
import io.izzel.arclight.common.mod.util.remapper.ClassLoaderAdapter;
import io.izzel.arclight.common.mod.util.remapper.ClassLoaderRemapper;
import org.objectweb.asm.Type;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.security.CodeSource;
import java.security.Permissions;
import java.security.ProtectionDomain;

@SuppressWarnings("unused")
public class ArclightReflectionHandler extends ClassLoader {

    private static final String PREFIX = "net.minecraft.";

    public static ClassLoaderRemapper remapper;

    // bukkit -> srg
    public static Class<?> redirectForName(String cl) throws ClassNotFoundException {
        return redirectForName(cl, true, remapper.getClassLoader());
    }

    // bukkit -> srg
    public static Class<?> redirectForName(String cl, boolean initialize, ClassLoader classLoader) throws ClassNotFoundException {
        if (!cl.startsWith(PREFIX)) {
            return Class.forName(cl, initialize, classLoader);
        }
        try {
            String replace = remapper.mapType(cl.replace('.', '/')).replace('/', '.');
            return Class.forName(replace, initialize, classLoader);
        } catch (ClassNotFoundException e) { // nested/inner class
            int i = cl.lastIndexOf('.');
            if (i > 0) {
                String replace = cl.substring(0, i).replace('.', '/') + "$" + cl.substring(i + 1);
                replace = remapper.mapType(replace).replace('/', '.').replace('$', '.');
                return Class.forName(replace, initialize, classLoader);
            } else throw e;
        }
    }

    // srg -> bukkit
    public static String redirectMethodGetName(Method method) {
        return remapper.tryMapMethodToBukkit(method.getDeclaringClass(), method);
    }

    // bukkit -> srg
    public static Method redirectGetMethod(Class<?> cl, String bukkitName, Class<?>... pTypes) throws NoSuchMethodException {
        if (ClassLoaderAdapter.isDefineClassMethod(cl, bukkitName, pTypes)) {
            Class<?>[] classes = new Class<?>[pTypes.length + 1];
            classes[0] = ClassLoader.class;
            System.arraycopy(pTypes, 0, classes, 1, pTypes.length);
            return ArclightReflectionHandler.class.getMethod(bukkitName, classes);
        }
        Method method = remapper.tryMapMethodToSrg(cl, bukkitName, pTypes);
        if (method != null) {
            return method;
        } else {
            return cl.getMethod(bukkitName, pTypes);
        }
    }

    // bukkit -> srg
    public static Method redirectGetDeclaredMethod(Class<?> cl, String bukkitName, Class<?>... pTypes) throws NoSuchMethodException {
        if (ClassLoaderAdapter.isDefineClassMethod(cl, bukkitName, pTypes)) {
            Class<?>[] classes = new Class<?>[pTypes.length + 1];
            classes[0] = ClassLoader.class;
            System.arraycopy(pTypes, 0, classes, 1, pTypes.length);
            return ArclightReflectionHandler.class.getDeclaredMethod(bukkitName, classes);
        }
        Method method = remapper.tryMapMethodToSrg(cl, bukkitName, pTypes);
        if (method != null) {
            return method;
        } else {
            return cl.getDeclaredMethod(bukkitName, pTypes);
        }
    }

    // srg -> bukkit
    public static String redirectFieldGetName(Field field) {
        return remapper.tryMapFieldToBukkit(field.getDeclaringClass(), field.getName(), field);
    }

    // bukkit -> srg
    public static Field redirectGetField(Class<?> cl, String bukkitName) throws NoSuchFieldException {
        String field = remapper.tryMapFieldToSrg(cl, bukkitName);
        return cl.getField(field);
    }

    // bukkit -> srg
    public static Field redirectGetDeclaredField(Class<?> cl, String bukkitName) throws NoSuchFieldException {
        String field = remapper.tryMapDecFieldToSrg(cl, bukkitName);
        return cl.getDeclaredField(field);
    }

    // srg -> bukkit
    public static String redirectClassGetName(Class<?> cl) {
        String internalName = Type.getInternalName(cl);
        Type type = Type.getObjectType(remapper.toBukkitRemapper().mapType(internalName));
        return type.getClassName();
    }

    // srg -> bukkit
    public static String redirectClassGetCanonicalName(Class<?> cl) {
        String canonicalName = cl.getCanonicalName();
        if (canonicalName == null) {
            return null;
        }
        if (cl.isArray()) {
            String name = redirectClassGetCanonicalName(cl.getComponentType());
            if (name == null) return null;
            return name + "[]";
        }
        Class<?> enclosingClass = cl.getEnclosingClass();
        if (enclosingClass == null) {
            return redirectClassGetName(cl);
        } else {
            String name = redirectClassGetCanonicalName(enclosingClass);
            if (name == null) return null;
            return name + "." + redirectClassGetSimpleName(cl);
        }
    }

    // srg -> bukkit
    public static String redirectClassGetSimpleName(Class<?> cl) {
        if (!cl.getName().startsWith(PREFIX)) {
            return cl.getSimpleName();
        }
        String simpleName = cl.getSimpleName();
        if (simpleName.length() == 0) {
            return simpleName; // anon class
        }
        Class<?> enclosingClass = cl.getEnclosingClass();
        if (enclosingClass == null) { // simple class / lambdas
            String mapped = redirectClassGetName(cl);
            return mapped.substring(mapped.lastIndexOf('.') + 1);
        } else { // nested class
            String outer = redirectClassGetName(enclosingClass);
            String inner = redirectClassGetName(cl);
            return inner.substring(outer.length() + 1);
        }
    }

    // srg -> bukkit
    public static String redirectPackageGetName(Package pkg) {
        String name = pkg.getName();
        if (name.startsWith(PREFIX)) {
            return PREFIX + "server." + ArclightVersion.current().packageName();
        } else {
            return name;
        }
    }

    // bukkit -> srg
    public static MethodType redirectFromDescStr(String desc, ClassLoader classLoader) {
        String methodDesc = remapper.mapMethodDesc(desc);
        return MethodType.fromMethodDescriptorString(methodDesc, classLoader);
    }

    // bukkit -> srg
    public static MethodHandle redirectFindStatic(MethodHandles.Lookup lookup, Class<?> cl, String name, MethodType methodType) throws NoSuchMethodException, IllegalAccessException {
        Method method = remapper.tryMapMethodToSrg(cl, name, methodType.parameterArray());
        if (method != null) {
            return lookup.findStatic(cl, method.getName(), methodType);
        } else {
            return lookup.findStatic(cl, name, methodType);
        }
    }

    // bukkit -> srg
    public static MethodHandle redirectFindVirtual(MethodHandles.Lookup lookup, Class<?> cl, String name, MethodType methodType) throws NoSuchMethodException, IllegalAccessException {
        if (ClassLoaderAdapter.isDefineClassMethod(cl, name, methodType)) {
            Class<?>[] pTypes = methodType.parameterArray();
            Class<?>[] classes = new Class<?>[pTypes.length + 1];
            classes[0] = ClassLoader.class;
            System.arraycopy(pTypes, 0, classes, 1, pTypes.length);
            return lookup.findStatic(ArclightReflectionHandler.class, name, MethodType.methodType(Class.class, classes));
        }
        Method method = remapper.tryMapMethodToSrg(cl, name, methodType.parameterArray());
        if (method != null) {
            return lookup.findVirtual(cl, method.getName(), methodType);
        } else {
            return lookup.findVirtual(cl, name, methodType);
        }
    }

    // bukkit -> srg
    public static MethodHandle redirectFindSpecial(MethodHandles.Lookup lookup, Class<?> cl, String name, MethodType methodType, Class<?> spec) throws NoSuchMethodException, IllegalAccessException {
        Method method = remapper.tryMapMethodToSrg(cl, name, methodType.parameterArray());
        if (method != null) {
            return lookup.findSpecial(cl, method.getName(), methodType, spec);
        } else {
            return lookup.findSpecial(cl, name, methodType, spec);
        }
    }

    // bukkit -> srg
    public static MethodHandle redirectFindGetter(MethodHandles.Lookup lookup, Class<?> cl, String name, Class<?> type) throws IllegalAccessException, NoSuchFieldException {
        String field = remapper.tryMapFieldToSrg(cl, name);
        return lookup.findGetter(cl, field, type);
    }

    // bukkit -> srg
    public static MethodHandle redirectFindSetter(MethodHandles.Lookup lookup, Class<?> cl, String name, Class<?> type) throws IllegalAccessException, NoSuchFieldException {
        String field = remapper.tryMapFieldToSrg(cl, name);
        return lookup.findSetter(cl, field, type);
    }

    // bukkit -> srg
    public static MethodHandle redirectFindStaticGetter(MethodHandles.Lookup lookup, Class<?> cl, String name, Class<?> type) throws IllegalAccessException, NoSuchFieldException {
        String field = remapper.tryMapFieldToSrg(cl, name);
        return lookup.findStaticGetter(cl, field, type);
    }

    // bukkit -> srg
    public static MethodHandle redirectFindStaticSetter(MethodHandles.Lookup lookup, Class<?> cl, String name, Class<?> type) throws IllegalAccessException, NoSuchFieldException {
        String field = remapper.tryMapFieldToSrg(cl, name);
        return lookup.findStaticSetter(cl, field, type);
    }

    // bukkit -> srg
    public static Class<?> redirectClassLoaderLoadClass(ClassLoader loader, String canonicalName) throws ClassNotFoundException {
        if (!canonicalName.startsWith(PREFIX)) {
            return loader.loadClass(canonicalName);
        }
        String replace = remapper.mapType(canonicalName.replace('.', '/')).replace('/', '.');
        return loader.loadClass(replace);
    }

    public static Object redirectDefineClassInvoke(Method method, Object src, Object[] param) throws Exception {
        if (method.getDeclaringClass() == ArclightReflectionHandler.class && method.getName().equals("defineClass")) {
            Object[] args = new Object[param.length + 1];
            args[0] = src;
            System.arraycopy(param, 0, args, 1, param.length);
            return method.invoke(null, args);
        } else return method.invoke(src, param);
    }

    public static Class<?> defineClass(ClassLoader loader, byte[] b, int off, int len) {
        return defineClass(loader, null, b, off, len);
    }

    public static Class<?> defineClass(ClassLoader loader, String name, byte[] b, int off, int len) {
        return defineClass(loader, name, b, off, len, (ProtectionDomain) null);
    }

    public static Class<?> defineClass(ClassLoader loader, String name, byte[] b, int off, int len, ProtectionDomain pd) {
        byte[] bytes = remapper.remapClass(b);
        return Unsafe.defineClass(name, bytes, 0, bytes.length, loader, pd);
    }

    public static Class<?> defineClass(ClassLoader loader, String name, ByteBuffer b, ProtectionDomain pd) {
        byte[] bytes = new byte[b.remaining()];
        b.get(bytes);
        return defineClass(loader, name, bytes, 0, bytes.length, pd);
    }

    public static Class<?> defineClass(ClassLoader loader, String name, byte[] b, int off, int len, CodeSource cs) {
        return defineClass(loader, name, b, off, len, new ProtectionDomain(cs, new Permissions()));
    }

    public static Class<?> defineClass(ClassLoader loader, String name, ByteBuffer b, CodeSource cs) {
        return defineClass(loader, name, b, new ProtectionDomain(cs, new Permissions()));
    }
}
