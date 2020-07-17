package io.izzel.arclight.common.mod.util.remapper.generated;

import io.izzel.arclight.common.mod.util.remapper.ClassLoaderRemapper;
import org.objectweb.asm.Type;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

@SuppressWarnings("unused")
public class ArclightReflectionHandler {

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
        Method method = remapper.tryMapMethodToSrg(cl, bukkitName, pTypes);
        if (method != null) {
            return method;
        } else {
            return cl.getMethod(bukkitName, pTypes);
        }
    }

    // bukkit -> srg
    public static Method redirectGetDeclaredMethod(Class<?> cl, String bukkitName, Class<?>... pTypes) throws NoSuchMethodException {
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
}
