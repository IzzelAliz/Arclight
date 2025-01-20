package io.izzel.arclight.boot.application;

import cpw.mods.cl.ModuleClassLoader;
import io.izzel.arclight.api.Unsafe;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

import java.io.IOException;
import java.security.ProtectionDomain;
import java.util.Map;
import java.util.function.BiFunction;

public class BootstrapTransformer {
    private static final Map<String, BiFunction<ClassLoader, ProtectionDomain, Exception>> SUPPORTED = Map.of(
            "cpw.mods.bootstraplauncher.BootstrapLauncher",
            BootstrapTransformer::transformBootstrapLauncher
    );

    public static Class<?> loadTransform(String className, ClassLoader cl, ProtectionDomain domain) throws Exception {
        if (!SUPPORTED.containsKey(className)) {
            throw new UnsupportedOperationException("Transformation for "+className+" is not supported");
        }
        var ex = SUPPORTED.get(className).apply(cl, domain);
        if (ex != null) throw ex;
        return cl.loadClass(className);
    }

    /*
     * Previous implementation of BootstrapLauncher relies on the order of ServiceLoader.load().stream()
     * where the ApplicationBootstrap will be ahead of modlauncher, which is an UB related to module name.
     * Modify BootstrapLauncher to use ApplicationBootstrap directly so a change in module name won't
     * affect launch process.
     */
    public static Exception transformBootstrapLauncher(ClassLoader cl, ProtectionDomain domain) {
        final var cpwClassName = "cpw.mods.bootstraplauncher.BootstrapLauncher";
        final var cpwClassFile = "cpw/mods/bootstraplauncher/BootstrapLauncher.class";
        System.out.println("Transforming " + cpwClassName);
        try(var inputStream = cl.getResourceAsStream(cpwClassFile)) {
            if (inputStream == null) {
                return new IOException("getResourceAsStream can't read BootstrapLauncher.class");
            }
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
                return new NullPointerException("Cannot find main(String[]) in BootstrapLauncher");
            }
            // Apply transformation
            var insns = asmMain.instructions;
            var injected = false;
            for (int i = 0; i < insns.size(); i++) {
                if (insns.get(i) instanceof MethodInsnNode invoke) {
                    if ("java/util/function/Consumer".equals(invoke.owner)
                            && "accept".equals(invoke.name)) {
                        // Raw: [SERVICE].accept(args);
                        // Modified: ((Consumer)new ApplicationBootstrap()).accept(args);
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
                        insns.insert(invoke, createArclightBoot);
                        insns.remove(invoke);
                        injected = true;
                        break;
                    }
                }
            }
            if (!injected) {
                return new Exception("BootstrapTransformer failed to transform BootstrapLauncher: Consumer.accept(String[]) not found");
            }
            // Save and define transformed class
            var cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            asmClass.accept(cw);
            var bytes = cw.toByteArray();
            Unsafe.defineClass(cpwClassName, bytes, 0, bytes.length, cl, domain);
        } catch (IOException e) {
            return e;
        }
        return null;
    }

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
}
