package io.izzel.arclight.common.mod.util.remapper;

import io.izzel.arclight.api.PluginPatcher;
import io.izzel.arclight.api.Unsafe;
import net.md_5.specialsource.repo.ClassRepo;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

import java.io.InputStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;

public class ClassLoaderRepo implements ClassRepo, PluginPatcher.ClassRepo {

    private static final ReferenceQueue<ClassLoader> STALE_LOADERS = new ReferenceQueue<>();

    private final LoaderReference classLoader;

    public ClassLoaderRepo(ClassLoader classLoader) {
        this.classLoader = new LoaderReference(classLoader, this);
    }

    static ClassLoaderRepo pollStaleRepo() {
        LoaderReference reference = (LoaderReference) STALE_LOADERS.poll();
        return reference == null ? null : reference.repo;
    }

    boolean isStale() {
        return this.classLoader.refersTo(null);
    }

    @Override
    public ClassNode findClass(String internalName) {
        return findClass(internalName, ClassReader.SKIP_CODE);
    }

    @Override
    public ClassNode findClass(String internalName, int parsingOptions) {
        ClassLoader classLoader = this.classLoader.get();
        if (classLoader == null) return null;
        try {
            URL url = classLoader instanceof URLClassLoader
                ? ((URLClassLoader) classLoader).findResource(internalName + ".class") // search local
                : (URL) H_FIND_RESOURCE.invokeExact(classLoader, internalName + ".class");
            if (url == null) return null;
            URLConnection connection = url.openConnection();
            try (InputStream inputStream = connection.getInputStream()) {
                ClassReader reader = new ClassReader(inputStream);
                ClassNode classNode = new ClassNode();
                reader.accept(classNode, parsingOptions);
                return classNode;
            }
        } catch (Throwable ignored) {
        } finally {
            Reference.reachabilityFence(classLoader);
        }
        return null;
    }

    private static final class LoaderReference extends WeakReference<ClassLoader> {

        private final ClassLoaderRepo repo;

        private LoaderReference(ClassLoader classLoader, ClassLoaderRepo repo) {
            super(classLoader, STALE_LOADERS);
            this.repo = repo;
        }
    }

    private static final MethodHandle H_FIND_RESOURCE;

    static {
        try {
            H_FIND_RESOURCE = Unsafe.lookup().findVirtual(ClassLoader.class, "findResource", MethodType.methodType(URL.class, String.class));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
