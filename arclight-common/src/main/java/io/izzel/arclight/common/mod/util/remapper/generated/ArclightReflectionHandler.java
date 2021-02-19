package io.izzel.arclight.common.mod.util.remapper.generated;

import com.google.common.collect.Iterators;
import io.izzel.arclight.api.ArclightVersion;
import io.izzel.arclight.api.Unsafe;
import io.izzel.arclight.common.mod.util.remapper.ArclightRedirectAdapter;
import io.izzel.arclight.common.mod.util.remapper.ClassLoaderRemapper;
import io.izzel.arclight.common.util.ArrayUtil;
import io.izzel.tools.product.Product4;
import org.apache.commons.collections.iterators.IteratorEnumeration;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;

import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.security.CodeSource;
import java.security.Permissions;
import java.security.ProtectionDomain;
import java.security.SecureClassLoader;
import java.util.Arrays;
import java.util.Enumeration;

@SuppressWarnings("unused")
public class ArclightReflectionHandler extends ClassLoader {

    private static final String PREFIX = "net.minecraft.";

    public static ClassLoaderRemapper remapper;

    // srg -> bukkit
    public static String redirectFieldGetName(Field field) {
        return remapper.tryMapFieldToBukkit(field.getDeclaringClass(), field.getName(), field);
    }

    // srg -> bukkit
    public static String redirectMethodGetName(Method method) {
        return remapper.tryMapMethodToBukkit(method.getDeclaringClass(), method);
    }

    // srg -> bukkit
    public static String redirectClassGetCanonicalName(Class<?> cl) {
        if (cl.isArray()) {
            String name = redirectClassGetCanonicalName(cl.getComponentType());
            if (name == null) return null;
            return name + "[]";
        }
        if (cl.isLocalClass() || cl.isAnonymousClass()) {
            return null;
        }
        String canonicalName = cl.getCanonicalName();
        if (canonicalName == null) {
            return null;
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
    public static String handleClassGetName(String cl) {
        return remapper.toBukkitRemapper().mapType(cl.replace('.', '/')).replace('/', '.');
    }

    // srg -> bukkit
    public static String redirectClassGetName(Class<?> cl) {
        String internalName = Type.getInternalName(cl);
        Type type = Type.getObjectType(remapper.toBukkitRemapper().mapType(internalName));
        return type.getInternalName().replace('/', '.');
    }

    // srg -> bukkit
    public static String handlePackageGetName(String name) {
        if (name.startsWith(PREFIX)) {
            return PREFIX + "server." + ArclightVersion.current().packageName();
        } else {
            return name;
        }
    }

    // srg -> bukkit
    public static String redirectPackageGetName(Package pkg) {
        return handlePackageGetName(pkg.getName());
    }

    // bukkit -> srg
    public static Class<?> redirectClassForName(String cl) throws ClassNotFoundException {
        return redirectClassForName(cl, true, Unsafe.getCallerClass().getClassLoader());
    }

    // bukkit -> srg
    public static Class<?> redirectClassForName(String cl, boolean initialize, ClassLoader classLoader) throws ClassNotFoundException {
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

    // bukkit -> srg
    public static Object[] handleClassGetField(Class<?> cl, String bukkitName) {
        return new Object[]{cl, remapper.tryMapFieldToSrg(cl, bukkitName)};
    }

    // bukkit -> srg
    public static Field redirectClassGetField(Class<?> cl, String bukkitName) throws NoSuchFieldException {
        String field = remapper.tryMapFieldToSrg(cl, bukkitName);
        return cl.getField(field);
    }

    // bukkit -> srg
    public static Object[] handleClassGetDeclaredField(Class<?> cl, String bukkitName) {
        return handleClassGetField(cl, bukkitName);
    }

    // bukkit -> srg
    public static Field redirectClassGetDeclaredField(Class<?> cl, String bukkitName) throws NoSuchFieldException {
        String field = remapper.tryMapDecFieldToSrg(cl, bukkitName);
        return cl.getDeclaredField(field);
    }

    // bukkit -> srg
    public static Object[] handleClassGetMethod(Class<?> cl, String bukkitName, Class<?>... pTypes) {
        Method method = remapper.tryMapMethodToSrg(cl, bukkitName, pTypes);
        return new Object[]{cl, method == null ? bukkitName : method.getName(), pTypes};
    }

    // bukkit -> srg
    public static Method redirectClassGetMethod(Class<?> cl, String bukkitName, Class<?>... pTypes) throws NoSuchMethodException {
        Method method = remapper.tryMapMethodToSrg(cl, bukkitName, pTypes);
        if (method != null) {
            return method;
        } else {
            return cl.getMethod(bukkitName, pTypes);
        }
    }

    // bukkit -> srg
    public static Object[] handleClassGetDeclaredMethod(Class<?> cl, String bukkitName, Class<?>... pTypes) {
        return handleClassGetMethod(cl, bukkitName, pTypes);
    }

    // bukkit -> srg
    public static Method redirectClassGetDeclaredMethod(Class<?> cl, String bukkitName, Class<?>... pTypes) throws NoSuchMethodException {
        Method method = remapper.tryMapMethodToSrg(cl, bukkitName, pTypes);
        if (method != null) {
            return method;
        } else {
            return cl.getDeclaredMethod(bukkitName, pTypes);
        }
    }

    // bukkit -> srg
    public static Object[] handleFromDescStr(String desc, ClassLoader classLoader) {
        return new Object[]{remapper.mapMethodDesc(desc), classLoader};
    }

    // bukkit -> srg
    public static MethodType redirectFromDescStr(String desc, ClassLoader classLoader) {
        String methodDesc = remapper.mapMethodDesc(desc);
        return MethodType.fromMethodDescriptorString(methodDesc, classLoader);
    }

    // bukkit -> srg
    public static Object[] handleLookupFindStatic(MethodHandles.Lookup lookup, Class<?> cl, String name, MethodType methodType) {
        Method method = remapper.tryMapMethodToSrg(cl, name, methodType.parameterArray());
        return new Object[]{lookup, cl, method == null ? name : method.getName(), methodType};
    }

    // bukkit -> srg
    public static MethodHandle redirectLookupFindStatic(MethodHandles.Lookup lookup, Class<?> cl, String name, MethodType methodType) throws NoSuchMethodException, IllegalAccessException {
        Method method = remapper.tryMapMethodToSrg(cl, name, methodType.parameterArray());
        if (method != null) {
            return lookup.findStatic(cl, method.getName(), methodType);
        } else {
            return lookup.findStatic(cl, name, methodType);
        }
    }

    // bukkit -> srg
    public static Object[] handleLookupFindVirtual(MethodHandles.Lookup lookup, Class<?> cl, String name, MethodType methodType) {
        return handleLookupFindStatic(lookup, cl, name, methodType);
    }

    // bukkit -> srg
    public static MethodHandle redirectLookupFindVirtual(MethodHandles.Lookup lookup, Class<?> cl, String name, MethodType methodType) throws NoSuchMethodException, IllegalAccessException {
        Method method = remapper.tryMapMethodToSrg(cl, name, methodType.parameterArray());
        if (method != null) {
            return lookup.findVirtual(cl, method.getName(), methodType);
        } else {
            return lookup.findVirtual(cl, name, methodType);
        }
    }

    // bukkit -> srg
    public static Object[] handleLookupFindSpecial(MethodHandles.Lookup lookup, Class<?> cl, String name, MethodType methodType, Class<?> spec) {
        Method method = remapper.tryMapMethodToSrg(cl, name, methodType.parameterArray());
        return new Object[]{lookup, cl, method == null ? name : method.getName(), methodType, spec};
    }

    // bukkit -> srg
    public static MethodHandle redirectLookupFindSpecial(MethodHandles.Lookup lookup, Class<?> cl, String name, MethodType methodType, Class<?> spec) throws NoSuchMethodException, IllegalAccessException {
        Method method = remapper.tryMapMethodToSrg(cl, name, methodType.parameterArray());
        if (method != null) {
            return lookup.findSpecial(cl, method.getName(), methodType, spec);
        } else {
            return lookup.findSpecial(cl, name, methodType, spec);
        }
    }

    // bukkit -> srg
    public static Object[] handleLookupFindGetter(MethodHandles.Lookup lookup, Class<?> cl, String name, Class<?> type) {
        String field = remapper.tryMapFieldToSrg(cl, name);
        return new Object[]{lookup, cl, field, type};
    }

    // bukkit -> srg
    public static MethodHandle redirectLookupFindGetter(MethodHandles.Lookup lookup, Class<?> cl, String name, Class<?> type) throws IllegalAccessException, NoSuchFieldException {
        String field = remapper.tryMapFieldToSrg(cl, name);
        return lookup.findGetter(cl, field, type);
    }

    // bukkit -> srg
    public static Object[] handleLookupFindSetter(MethodHandles.Lookup lookup, Class<?> cl, String name, Class<?> type) {
        return handleLookupFindGetter(lookup, cl, name, type);
    }

    // bukkit -> srg
    public static MethodHandle redirectLookupFindSetter(MethodHandles.Lookup lookup, Class<?> cl, String name, Class<?> type) throws IllegalAccessException, NoSuchFieldException {
        String field = remapper.tryMapFieldToSrg(cl, name);
        return lookup.findSetter(cl, field, type);
    }

    // bukkit -> srg
    public static Object[] handleLookupFindStaticGetter(MethodHandles.Lookup lookup, Class<?> cl, String name, Class<?> type) {
        return handleLookupFindGetter(lookup, cl, name, type);
    }

    // bukkit -> srg
    public static MethodHandle redirectLookupFindStaticGetter(MethodHandles.Lookup lookup, Class<?> cl, String name, Class<?> type) throws IllegalAccessException, NoSuchFieldException {
        String field = remapper.tryMapFieldToSrg(cl, name);
        return lookup.findStaticGetter(cl, field, type);
    }

    // bukkit -> srg
    public static Object[] handleLookupFindStaticSetter(MethodHandles.Lookup lookup, Class<?> cl, String name, Class<?> type) {
        return handleLookupFindGetter(lookup, cl, name, type);
    }

    // bukkit -> srg
    public static MethodHandle redirectLookupFindStaticSetter(MethodHandles.Lookup lookup, Class<?> cl, String name, Class<?> type) throws IllegalAccessException, NoSuchFieldException {
        String field = remapper.tryMapFieldToSrg(cl, name);
        return lookup.findStaticSetter(cl, field, type);
    }

    public static Object[] handleClassLoaderLoadClass(ClassLoader loader, String binaryName) {
        return new Object[]{loader, remapper.mapType(binaryName.replace('.', '/')).replace('/', '.')};
    }

    // bukkit -> srg
    public static Class<?> redirectClassLoaderLoadClass(ClassLoader loader, String binaryName) throws ClassNotFoundException {
        String replace = remapper.mapType(binaryName.replace('.', '/')).replace('/', '.');
        return loader.loadClass(replace);
    }

    public static String findMappedResource(Class<?> cl, String name) {
        if (name.isEmpty() || !name.endsWith(".class")) return null;
        name = name.substring(0, name.length() - 6);
        String className;
        if (cl != null) {
            if (name.charAt(0) == '/') {
                className = name.substring(1);
            } else {
                Class<?> c = cl;
                while (c.isArray()) c = c.getComponentType();
                String mapped = remapper.toBukkitRemapper().mapType(c.getName().replace('.', '/'));
                int index = mapped.lastIndexOf('/');
                if (index == -1) {
                    className = name;
                } else {
                    className = mapped.substring(0, index) + '/' + name;
                }
            }
        } else {
            className = name;
        }
        className = remapper.mapType(className);
        if (className.startsWith("java/")) return null;
        else if (cl != null) return "/" + className + ".class";
        else return className + ".class";
    }

    public static URL redirectClassGetResource(Class<?> cl, String name) throws MalformedURLException {
        String mappedResource = findMappedResource(cl, name);
        if (mappedResource == null) {
            return cl.getResource(name);
        } else {
            URL resource = cl.getResource(mappedResource);
            return resource == null ? null : new URL("remap:" + resource);
        }
    }

    public static InputStream redirectClassGetResourceAsStream(Class<?> cl, String name) throws IOException {
        String mappedResource = findMappedResource(cl, name);
        if (mappedResource == null) {
            return cl.getResourceAsStream(name);
        } else {
            URL resource = cl.getResource(mappedResource);
            if (resource == null) return null;
            return new URL("remap:" + resource).openStream();
        }
    }

    public static URL redirectClassLoaderGetResource(ClassLoader loader, String name) throws MalformedURLException {
        String mappedResource = findMappedResource(null, name);
        if (mappedResource == null) {
            return loader.getResource(name);
        } else {
            URL resource = loader.getResource(mappedResource);
            return resource == null ? null : new URL("remap:" + resource);
        }
    }

    public static Enumeration<URL> redirectClassLoaderGetResources(ClassLoader loader, String name) throws IOException {
        String mappedResource = findMappedResource(null, name);
        if (mappedResource == null) {
            return loader.getResources(name);
        } else {
            URL resource = loader.getResource(mappedResource);
            return resource == null ? null : new IteratorEnumeration(
                Iterators.singletonIterator(new URL("remap:" + resource)));
        }
    }

    public static InputStream redirectClassLoaderGetResourceAsStream(ClassLoader loader, String name) throws IOException {
        URL url = redirectClassLoaderGetResource(loader, name);
        if (url == null) {
            return null;
        } else {
            return url.openStream();
        }
    }

    public static Object[] handleMethodInvoke(Method method, Object src, Object[] param) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Product4<String, Class<?>[], String, Class<?>[]> invokeRule = ArclightRedirectAdapter.getInvokeRule(method);
        if (invokeRule != null) {
            if (invokeRule._3 != null && method.getParameterCount() > 0) {
                Method handleMethod = remapper.getGeneratedHandlerClass().getMethod(invokeRule._3, invokeRule._4);
                if (handleMethod.getReturnType().isArray() && !Modifier.isStatic(method.getModifiers())) {
                    Object[] invoke = (Object[]) handleMethod.invoke(null, ArrayUtil.prepend(param, src));
                    return new Object[]{method, invoke[0], Arrays.copyOfRange(invoke, 1, invoke.length)};
                } else {
                    return new Object[]{method, src, handleMethod.invoke(null, param)};
                }
            } else {
                Method redirectMethod = remapper.getGeneratedHandlerClass().getMethod(invokeRule._1, invokeRule._2);
                return new Object[]{redirectMethod, null, ArrayUtil.prepend(param, src)};
            }
        } else {
            return new Object[]{method, src, param};
        }
    }

    public static Object redirectMethodInvoke(Method method, Object src, Object[] param) throws Throwable {
        Product4<String, Class<?>[], String, Class<?>[]> invokeRule = ArclightRedirectAdapter.getInvokeRule(method);
        if (invokeRule != null) {
            if (invokeRule._3 != null && method.getParameterCount() > 0) {
                Method handleMethod = remapper.getGeneratedHandlerClass().getMethod(invokeRule._3, invokeRule._4);
                if (handleMethod.getReturnType().isArray()) {
                    if (Modifier.isStatic(method.getModifiers())) {
                        return method.invoke(src, (Object[]) handleMethod.invoke(null, param));
                    } else {
                        Object[] result = (Object[]) handleMethod.invoke(null, ArrayUtil.prepend(param, src));
                        return method.invoke(result[0], Arrays.copyOfRange(result, 1, result.length));
                    }
                } else {
                    return method.invoke(src, handleMethod.invoke(null, param));
                }
            } else {
                Method redirectMethod = remapper.getGeneratedHandlerClass().getMethod(invokeRule._1, invokeRule._2);
                return redirectMethod.invoke(null, ArrayUtil.prepend(param, src));
            }
        } else {
            return method.invoke(src, param);
        }
    }

    public static Object[] handleDefineClass(ClassLoader loader, byte[] b, int off, int len) {
        byte[] bytes = remapper.remapClass(b);
        return new Object[]{loader, bytes, 0, bytes.length};
    }

    public static Class<?> redirectDefineClass(ClassLoader loader, byte[] b, int off, int len) {
        return redirectDefineClass(loader, null, b, off, len);
    }

    public static Object[] handleDefineClass(ClassLoader loader, String name, byte[] b, int off, int len) {
        byte[] bytes = remapper.remapClass(b);
        return new Object[]{loader, new ClassReader(bytes).getClassName().replace('/', '.'), bytes, 0, bytes.length};
    }

    public static Class<?> redirectDefineClass(ClassLoader loader, String name, byte[] b, int off, int len) {
        return redirectDefineClass(loader, name, b, off, len, (ProtectionDomain) null);
    }

    public static Object[] handleDefineClass(ClassLoader loader, String name, byte[] b, int off, int len, ProtectionDomain pd) {
        byte[] bytes = remapper.remapClass(b);
        return new Object[]{loader, new ClassReader(bytes).getClassName().replace('/', '.'), bytes, 0, bytes.length, pd};
    }

    public static Class<?> redirectDefineClass(ClassLoader loader, String name, byte[] b, int off, int len, ProtectionDomain pd) {
        byte[] bytes = remapper.remapClass(b);
        return Unsafe.defineClass(new ClassReader(bytes).getClassName().replace('/', '.'), bytes, 0, bytes.length, loader, pd);
    }

    public static Object[] handleDefineClass(ClassLoader loader, String name, ByteBuffer b, ProtectionDomain pd) {
        byte[] bytes = new byte[b.remaining()];
        b.get(bytes);
        bytes = remapper.remapClass(bytes);
        return new Object[]{loader, new ClassReader(bytes).getClassName().replace('/', '.'), ByteBuffer.wrap(bytes), pd};
    }

    public static Class<?> redirectDefineClass(ClassLoader loader, String name, ByteBuffer b, ProtectionDomain pd) {
        byte[] bytes = new byte[b.remaining()];
        b.get(bytes);
        return redirectDefineClass(loader, name, bytes, 0, bytes.length, pd);
    }

    public static Object[] handleDefineClass(SecureClassLoader loader, String name, byte[] b, int off, int len, CodeSource cs) {
        byte[] bytes = remapper.remapClass(b);
        return new Object[]{loader, new ClassReader(bytes).getClassName().replace('/', '.'), bytes, 0, bytes.length, cs};
    }

    public static Class<?> redirectDefineClass(SecureClassLoader loader, String name, byte[] b, int off, int len, CodeSource cs) {
        return redirectDefineClass(loader, name, b, off, len, new ProtectionDomain(cs, new Permissions()));
    }

    public static Object[] handleDefineClass(SecureClassLoader loader, String name, ByteBuffer b, CodeSource cs) {
        byte[] bytes = new byte[b.remaining()];
        b.get(bytes);
        bytes = remapper.remapClass(bytes);
        return new Object[]{loader, new ClassReader(bytes).getClassName().replace('/', '.'), ByteBuffer.wrap(bytes), cs};
    }

    public static Class<?> redirectDefineClass(SecureClassLoader loader, String name, ByteBuffer b, CodeSource cs) {
        return redirectDefineClass(loader, name, b, new ProtectionDomain(cs, new Permissions()));
    }
}
