package io.izzel.arclight.common.mod.util.remapper.generated;

import io.izzel.arclight.api.Unsafe;
import io.izzel.arclight.common.mod.util.remapper.*;
import io.izzel.arclight.common.util.Enumerations;
import org.objectweb.asm.ClassReader;

import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.security.CodeSource;
import java.security.Permissions;
import java.security.ProtectionDomain;
import java.security.SecureClassLoader;
import java.util.Enumeration;

@SuppressWarnings("unused")
public class ArclightReflectionHandler extends ClassLoader {

    public static ClassLoaderRemapper remapper;

    public static Method[] redirectGetDeclaredMethods(Class<?> cl) {
        try {
            return cl.getDeclaredMethods();
        } catch (TypeNotPresentException e) {
            if (e.getCause() instanceof ClassNotFoundException) {
                remapper.tryDefineClass(e.getCause().getMessage().replace('.', '/'));
                return redirectGetDeclaredMethods(cl);
            } else throw e;
        } catch (NoClassDefFoundError error) {
            remapper.tryDefineClass(error.getMessage());
            return redirectGetDeclaredMethods(cl);
        }
    }

    public static Method[] redirectGetMethods(Class<?> cl) {
        try {
            return cl.getMethods();
        } catch (TypeNotPresentException e) {
            if (e.getCause() instanceof ClassNotFoundException) {
                remapper.tryDefineClass(e.getCause().getMessage().replace('.', '/'));
                return redirectGetMethods(cl);
            } else throw e;
        } catch (NoClassDefFoundError error) {
            remapper.tryDefineClass(error.getMessage());
            return redirectGetMethods(cl);
        }
    }

    public static Field[] redirectGetDeclaredFields(Class<?> cl) {
        try {
            return cl.getDeclaredFields();
        } catch (TypeNotPresentException e) {
            if (e.getCause() instanceof ClassNotFoundException) {
                remapper.tryDefineClass(e.getCause().getMessage().replace('.', '/'));
                return redirectGetDeclaredFields(cl);
            } else throw e;
        } catch (NoClassDefFoundError error) {
            remapper.tryDefineClass(error.getMessage());
            return redirectGetDeclaredFields(cl);
        }
    }

    public static Field[] redirectGetFields(Class<?> cl) {
        try {
            return cl.getFields();
        } catch (TypeNotPresentException e) {
            if (e.getCause() instanceof ClassNotFoundException) {
                remapper.tryDefineClass(e.getCause().getMessage().replace('.', '/'));
                return redirectGetFields(cl);
            } else throw e;
        } catch (NoClassDefFoundError error) {
            remapper.tryDefineClass(error.getMessage());
            return redirectGetFields(cl);
        }
    }

    public static String findClassResource(Class<?> cl, String name) {
        if (name.isEmpty() || !name.endsWith(".class")) return null;
        name = name.substring(0, name.length() - 6);
        String className;
        if (cl != null) {
            if (name.charAt(0) == '/') {
                className = name.substring(1);
            } else {
                Class<?> c = cl;
                while (c.isArray()) c = c.getComponentType();
                String ownerName = c.getName().replace('.', '/');
                int index = ownerName.lastIndexOf('/');
                if (index == -1) {
                    className = name;
                } else {
                    className = ownerName.substring(0, index) + '/' + name;
                }
            }
        } else {
            className = name;
        }
        if (className.startsWith("java/") || className.startsWith("jdk/") || className.startsWith("javax/")) {
            return null;
        } else if (cl != null) return "/" + className + ".class";
        else return className + ".class";
    }

    public static URL redirectClassGetResource(Class<?> cl, String name) throws MalformedURLException {
        String classResource = findClassResource(cl, name);
        if (classResource == null) {
            if (name.startsWith("/java/") || name.startsWith("/jdk/") || name.startsWith("/javax/")) {
                return ClassLoader.getPlatformClassLoader().getResource(name);
            }
            return cl.getResource(name);
        } else {
            URL resource = cl.getResource(classResource);
            return resource == null ? null : new URL("remap:" + resource);
        }
    }

    public static InputStream redirectClassGetResourceAsStream(Class<?> cl, String name) throws IOException {
        String classResource = findClassResource(cl, name);
        if (classResource == null) {
            if (name.startsWith("/java/") || name.startsWith("/jdk/") || name.startsWith("/javax/")) {
                return ClassLoader.getPlatformClassLoader().getResourceAsStream(name);
            }
            return cl.getResourceAsStream(name);
        } else {
            URL resource = cl.getResource(classResource);
            if (resource == null) return null;
            return new URL("remap:" + resource).openStream();
        }
    }

    public static URL redirectClassLoaderGetResource(ClassLoader loader, String name) throws MalformedURLException {
        String classResource = findClassResource(null, name);
        if (classResource == null) {
            if (name.startsWith("java/") || name.startsWith("jdk/") || name.startsWith("javax/")) {
                return ClassLoader.getPlatformClassLoader().getResource(name);
            }
            return loader.getResource(name);
        } else {
            URL resource = loader.getResource(classResource);
            return resource == null ? null : new URL("remap:" + resource);
        }
    }

    public static Enumeration<URL> redirectClassLoaderGetResources(ClassLoader loader, String name) throws IOException {
        String classResource = findClassResource(null, name);
        if (classResource == null) {
            if (name.startsWith("java/") || name.startsWith("jdk/") || name.startsWith("javax/")) {
                return ClassLoader.getPlatformClassLoader().getResources(name);
            }
            return loader.getResources(name);
        } else {
            Enumeration<URL> resources = loader.getResources(classResource);
            return Enumerations.remapped(resources);
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

    public static Object[] handleMethodInvoke(Method method, Object src, Object[] param) throws Throwable {
        Object[] ret = ArclightRedirectAdapter.runHandle(remapper, method, src, param);
        if (ret != null) {
            return ret;
        } else {
            return new Object[]{method, src, param};
        }
    }

    public static Object redirectMethodInvoke(Method method, Object src, Object[] param) throws Throwable {
        Object ret = ArclightRedirectAdapter.runRedirect(remapper, method, src, param);
        if (ret != remapper) {
            return ret;
        } else {
            return method.invoke(src, param);
        }
    }

    public static Object[] handleDefineClass(ClassLoader loader, byte[] b, int off, int len) {
        byte[] bytes = transformOrAdd(loader, b);
        return new Object[]{loader, bytes, 0, bytes.length};
    }

    public static Class<?> redirectDefineClass(ClassLoader loader, byte[] b, int off, int len) {
        return redirectDefineClass(loader, null, b, off, len);
    }

    public static Object[] handleDefineClass(ClassLoader loader, String name, byte[] b, int off, int len) {
        byte[] bytes = transformOrAdd(loader, b);
        return new Object[]{loader, new ClassReader(bytes).getClassName().replace('/', '.'), bytes, 0, bytes.length};
    }

    public static Class<?> redirectDefineClass(ClassLoader loader, String name, byte[] b, int off, int len) {
        return redirectDefineClass(loader, name, b, off, len, null);
    }

    public static Object[] handleDefineClass(ClassLoader loader, String name, byte[] b, int off, int len, ProtectionDomain pd) {
        byte[] bytes = transformOrAdd(loader, b);
        return new Object[]{loader, new ClassReader(bytes).getClassName().replace('/', '.'), bytes, 0, bytes.length, pd};
    }

    public static Class<?> redirectDefineClass(ClassLoader loader, String name, byte[] b, int off, int len, ProtectionDomain pd) {
        byte[] bytes = transformOrAdd(loader, b);
        return Unsafe.defineClass(new ClassReader(bytes).getClassName().replace('/', '.'), bytes, 0, bytes.length, loader, pd);
    }

    public static Object[] handleDefineClass(ClassLoader loader, String name, ByteBuffer b, ProtectionDomain pd) {
        byte[] bytes = new byte[b.remaining()];
        b.get(bytes);
        bytes = transformOrAdd(loader, bytes);
        return new Object[]{loader, new ClassReader(bytes).getClassName().replace('/', '.'), ByteBuffer.wrap(bytes), pd};
    }

    public static Class<?> redirectDefineClass(ClassLoader loader, String name, ByteBuffer b, ProtectionDomain pd) {
        byte[] bytes = new byte[b.remaining()];
        b.get(bytes);
        return redirectDefineClass(loader, name, bytes, 0, bytes.length, pd);
    }

    public static Object[] handleDefineClass(SecureClassLoader loader, String name, byte[] b, int off, int len, CodeSource cs) {
        byte[] bytes = transformOrAdd(loader, b);
        return new Object[]{loader, new ClassReader(bytes).getClassName().replace('/', '.'), bytes, 0, bytes.length, cs};
    }

    public static Class<?> redirectDefineClass(SecureClassLoader loader, String name, byte[] b, int off, int len, CodeSource cs) {
        return redirectDefineClass(loader, name, b, off, len, new ProtectionDomain(cs, new Permissions()));
    }

    public static Object[] handleDefineClass(SecureClassLoader loader, String name, ByteBuffer b, CodeSource cs) {
        byte[] bytes = new byte[b.remaining()];
        b.get(bytes);
        bytes = transformOrAdd(loader, bytes);
        return new Object[]{loader, new ClassReader(bytes).getClassName().replace('/', '.'), ByteBuffer.wrap(bytes), cs};
    }

    public static Class<?> redirectDefineClass(SecureClassLoader loader, String name, ByteBuffer b, CodeSource cs) {
        return redirectDefineClass(loader, name, b, new ProtectionDomain(cs, new Permissions()));
    }

    public static Object[] handleUnsafeDefineClass(Object unsafe, String name, byte[] bytes, int off, int len, ClassLoader loader, ProtectionDomain pd) {
        bytes = transformOrAdd(loader, bytes);
        return new Object[]{unsafe, new ClassReader(bytes).getClassName().replace('/', '.'), bytes, 0, bytes.length, loader, pd};
    }

    public static Class<?> redirectUnsafeDefineClass(Object unsafe, String name, byte[] bytes, int off, int len, ClassLoader loader, ProtectionDomain pd) {
        return redirectDefineClass(loader, name, bytes, off, len, pd);
    }

    public static Object[] handleLookupDefineClass(MethodHandles.Lookup lookup, byte[] bytes) {
        return new Object[]{lookup, transformOrAdd(lookup.lookupClass().getClassLoader(), bytes)};
    }

    public static Class<?> redirectLookupDefineClass(MethodHandles.Lookup lookup, byte[] bytes) throws Throwable {
        byte[] transform = transformOrAdd(lookup.lookupClass().getClassLoader(), bytes);
        MethodHandle mh = Unsafe.lookup().findVirtual(MethodHandles.Lookup.class, "defineClass", MethodType.methodType(Class.class, byte[].class));
        return (Class<?>) mh.invokeExact(lookup, transform);
    }

    public static Object[] handleLookupDefineHiddenClass(MethodHandles.Lookup lookup, byte[] bytes, boolean initialize, MethodHandles.Lookup.ClassOption[] options) {
        return new Object[]{lookup, transformOrAdd(lookup.lookupClass().getClassLoader(), bytes), initialize, options};
    }

    public static MethodHandles.Lookup redirectLookupDefineHiddenClass(MethodHandles.Lookup lookup, byte[] bytes, boolean initialize, MethodHandles.Lookup.ClassOption[] options) throws IllegalAccessException {
        byte[] transform = transformOrAdd(lookup.lookupClass().getClassLoader(), bytes);
        return lookup.defineHiddenClass(transform, initialize, options);
    }

    public static Object[] handleLookupDefineHiddenClassWithClassData(MethodHandles.Lookup lookup, byte[] bytes, Object classData, boolean initialize, MethodHandles.Lookup.ClassOption[] options) {
        byte[] transform = transformOrAdd(lookup.lookupClass().getClassLoader(), bytes);
        return new Object[]{lookup, transform, classData, initialize, options};
    }

    public static MethodHandles.Lookup redirectLookupDefineHiddenClassWithClassData(MethodHandles.Lookup lookup, byte[] bytes, Object classData, boolean initialize, MethodHandles.Lookup.ClassOption[] options) throws IllegalAccessException {
        byte[] transform = transformOrAdd(lookup.lookupClass().getClassLoader(), bytes);
        return lookup.defineHiddenClassWithClassData(transform, classData, initialize, options);
    }

    public static byte[] transformOrAdd(ClassLoader loader, byte[] bytes) {
        RemappingClassLoader rcl = null;
        while (loader != null) {
            if (loader instanceof RemappingClassLoader) {
                rcl = ((RemappingClassLoader) loader);
                break;
            } else {
                loader = loader.getParent();
            }
        }
        if (rcl != null) {
            // Don't transform for remap=false.
            // We don't have ReflectionHandler in their ClassLoader.
            var repo = new ClassRepoWrapper(GlobalClassRepo.INSTANCE, rcl.getRemapConfig());
            return rcl.getRemapper().remapClassFile(bytes, repo, true);
        } else {
            ArclightRedirectAdapter.scanMethod(bytes);
            return bytes;
        }
    }
}
