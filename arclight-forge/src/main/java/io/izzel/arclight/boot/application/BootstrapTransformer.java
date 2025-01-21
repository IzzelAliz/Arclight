package io.izzel.arclight.boot.application;

import cpw.mods.cl.ModuleClassLoader;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

import java.io.IOException;
import java.io.InputStream;
import java.security.ProtectionDomain;

/*
 * The implementation is affected by BootstrapLauncher and ModLauncher
 * Be sure to check for updates.
 */
public class BootstrapTransformer extends ClassLoader {

    private static final String cpwClass = "cpw.mods.bootstraplauncher.BootstrapLauncher";

    private final ProtectionDomain domain = getClass().getProtectionDomain();

    @SuppressWarnings({"unused", "unchecked"})
    public static void onInvoke$BootstrapLauncher(String[] args, ModuleClassLoader moduleCl) {
        try {
            Class<ApplicationBootstrap> arclightBootClz = (Class<ApplicationBootstrap>) moduleCl.loadClass("io.izzel.arclight.boot.application.ApplicationBootstrap");
            Object instance = arclightBootClz.getConstructor().newInstance();
            arclightBootClz.getMethod("accept", String[].class).invoke(instance, (Object) args);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public BootstrapTransformer(ClassLoader appClassLoader) {
        super("arclight_bootstrap", appClassLoader);
    }

    /*
     * The class to transform can be resolved by AppClassLoader.
     * We have to break the delegation model to intercept class loading.
     */
    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            Class<?> c = findLoadedClass(name);
            if (c != null) {
                return c;
            }

            // The class is not loaded. Are we going to intercept?
            // The inner classes and the outer class should be loaded
            // in the same ClassLoader to avoid inter-module access issues.
            if (!name.contains(cpwClass)) {
                // Delegate to parent.
                // parent.loadClass is inaccessible from here.
                // This ClassLoader will only load the launcher
                // and then a new ClassLoader, whose parent is
                // platform ClassLoader (null), will load the game.
                return super.loadClass(name, resolve);
            }

            Class<?> clz;
            try {
                clz = loadTransform(name);
            } catch (IOException e) {
                e.printStackTrace();
                throw new ClassNotFoundException("Unexpected exception loading " + name);
            }

            if (resolve) {
                resolveClass(clz);
            }
            return clz;
        }
    }

    /*
     * findClass() is invoked when parent (in this case AppClassLoader)
     * cannot find the corresponding class. In this case we can't find either.
     */
    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        throw new ClassNotFoundException(name);
    }

    public Class<?> loadTransform(String className) throws IOException {
        if (className.equals(cpwClass)) {
            var file = cpwClass.replace('.', '/').concat(".class");
            try (var inputStream = getResourceAsStream(file)) {
                if (inputStream == null) {
                    throw new RuntimeException("getResourceAsStream can't read BootstrapLauncher.class");
                }
                var transformed = transformBootstrapLauncher(inputStream);
                return defineClass(cpwClass, transformed, 0, transformed.length, domain);
            }
        } else if (className.contains(cpwClass)) {
            var file = className.replace('.', '/').concat(".class");
            try (var inputStream = getResourceAsStream(file)) {
                if (inputStream == null) {
                    throw new RuntimeException("getResourceAsStream can't read "+file.substring(file.lastIndexOf('/')));
                }
                var bytes = inputStream.readAllBytes();
                return defineClass(className, bytes, 0, bytes.length, domain);
            }
        }
        throw new UnsupportedOperationException("Transformation for " + className + " is not supported");
    }

    /*
     * Previous implementation of BootstrapLauncher relies on the order of ServiceLoader.load().stream()
     * where the ApplicationBootstrap will be ahead of modlauncher, which is an UB related to module name.
     * Modify BootstrapLauncher to use ApplicationBootstrap directly so a change in module name won't
     * affect launch process.
     */
    public byte[] transformBootstrapLauncher(InputStream inputStream) throws IOException {
        System.out.println("Transforming cpw.mods.bootstraplauncher.BootstrapLauncher");
        var asmClass = new ClassNode();
        new ClassReader(inputStream).accept(asmClass, 0);

        // Find main(String[])
        MethodNode asmMain = null;
        for (var asmMethod : asmClass.methods) {
            if ("main".equals(asmMethod.name)) {
                asmMain = asmMethod;
                break;
            }
        }
        if (asmMain == null) {
            throw new RuntimeException("Cannot find main(String[]) in BootstrapLauncher");
        }

        // Find Consumer.accept(...)
        var insns = asmMain.instructions;
        MethodInsnNode injectionPoint = null;
        for (int i = 0; i < insns.size(); i++) {
            if (insns.get(i) instanceof MethodInsnNode invoke) {
                if ("java/util/function/Consumer".equals(invoke.owner)
                        && "accept".equals(invoke.name)) {
                    injectionPoint = invoke;
                    break;
                }
            }
        }
        if (injectionPoint == null) {
            throw new RuntimeException("BootstrapTransformer failed to transform BootstrapLauncher: Consumer.accept(String[]) not found");
        }

        // Apply transformation
        // Raw: [SERVICE].accept(args);
        // Modified: BootstrapTransformer.onInvoke$BootstrapLauncher(...);
        var createArclightBoot = new InsnList();
        {
            var popArgsThenService = new InsnNode(Opcodes.POP2);
            var aloadArgs = new VarInsnNode(Opcodes.ALOAD, 0);
            var aloadModuleCl = new VarInsnNode(Opcodes.ALOAD, 15);
            var onInvoke = new MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    "io/izzel/arclight/boot/application/BootstrapTransformer",
                    "onInvoke$BootstrapLauncher",
                    "([Ljava/lang/String;Lcpw/mods/cl/ModuleClassLoader;)V"
            );
            createArclightBoot.add(popArgsThenService);
            createArclightBoot.add(aloadArgs);
            createArclightBoot.add(aloadModuleCl);
            createArclightBoot.add(onInvoke);
        }
        insns.insert(injectionPoint, createArclightBoot);
        insns.remove(injectionPoint);

        // Save transformed class
        var cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        asmClass.accept(cw);
        return cw.toByteArray();
    }
}
