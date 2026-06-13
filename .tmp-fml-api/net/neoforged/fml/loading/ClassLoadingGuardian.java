/*
 * Copyright (c) NeoForged and contributors
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.loading;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.neoforged.fml.loading.moddiscovery.ModFile;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.util.Constants;

/**
 * Implements protections against accidentally loading classes owned by mods or Minecraft from any classloader
 * but a given, blessed loader (the transforming class loader).
 */
final class ClassLoadingGuardian implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClassLoadingGuardian.class);

    private final Instrumentation instrumentation;
    private final Set<String> protectedPackages;
    private final ClassFileTransformer guardianTransformer;
    private volatile boolean uninstalled;
    private volatile ClassLoader allowedClassLoader;

    public ClassLoadingGuardian(Instrumentation instrumentation, List<ModFile> gameContent) {
        this.instrumentation = instrumentation;
        this.protectedPackages = getPackages(gameContent);
        this.guardianTransformer = new ClassFileTransformer() {
            @Override
            public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) {
                throw new UnsupportedOperationException();
            }

            @Override
            public byte[] transform(Module module,
                    ClassLoader loader,
                    String className,
                    Class<?> classBeingRedefined,
                    ProtectionDomain protectionDomain,
                    byte[] classfileBuffer) {
                if (uninstalled) {
                    return null; // This can happen due to multi-threaded class-loading
                }

                if (loader == allowedClassLoader) {
                    return null;
                }

                var packageName = getPackageName(className);
                if (packageName != null && protectedPackages.contains(packageName)) {
                    LOGGER.error("Illegal load of protected class {} into class-loader {}", className, loader, new Throwable());

                    // Transformers are actually not allowed to throw. So we have to
                    // construct class bytecode that ruins the day for everyone.
                    ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
                    cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, className.replace('.', '/'), null,
                            Type.getInternalName(Object.class), null);

                    // create init method
                    MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, Constants.CLINIT, "()V", null, null);
                    mv.visitCode();
                    mv.visitMethodInsn(Opcodes.INVOKESTATIC, Type.getInternalName(ClassLoadingGuardian.class), "fail", "()V", false);
                    mv.visitInsn(Opcodes.RETURN);
                    mv.visitMaxs(0, 0);
                    mv.visitEnd();

                    cw.visitEnd();
                    return cw.toByteArray();
                }

                return null;
            }
        };
        this.instrumentation.addTransformer(guardianTransformer);
    }

    /**
     * NOTE: this is referenced by the generated self-destructing classes when an incorrect
     * class is accessed via the transformer.
     */
    public static void fail() {
        throw new IllegalStateException();
    }

    public void setAllowedClassLoader(ClassLoader allowedClassLoader) {
        this.allowedClassLoader = allowedClassLoader;

        // Final check for class-loading bugs
        var foundIssues = new ArrayList<Class<?>>();
        for (var loadedClass : instrumentation.getAllLoadedClasses()) {
            if (loadedClass.getClassLoader() == null) {
                continue; // JDK built-in
            }

            var physicalPackage = loadedClass.getPackageName().replace('.', '/');
            if (protectedPackages.contains(physicalPackage)) {
                // It's ok if the class is not reachable from the now current class-loader,
                // since we get reported ALL loaded classes, they may be unrelated class-loader hierarchies,
                // especially in testing scenarios.
                if (isReachableFrom(loadedClass.getClassLoader(), allowedClassLoader)) {
                    foundIssues.add(loadedClass);
                }
            }
        }

        if (!foundIssues.isEmpty()) {
            var message = new StringBuilder();
            message.append("Classes were loaded on the wrong class-loader:\n");
            for (var offendingClass : foundIssues) {
                message.append(' ').append(offendingClass.getName()).append(" from ").append(offendingClass.getClassLoader()).append('\n');
            }
            throw new IllegalArgumentException(message.toString());
        }
    }

    public void close() {
        if (!uninstalled) {
            uninstalled = true;
            instrumentation.removeTransformer(guardianTransformer);
            allowedClassLoader = null;
        }
    }

    private static Set<String> getPackages(List<ModFile> gameContent) {
        var protectedPackages = new HashSet<String>(1000);
        for (var modFile : gameContent) {
            for (var pkgName : modFile.getModuleDescriptor().packages()) {
                protectedPackages.add(pkgName.replace('.', '/'));
            }
        }
        return protectedPackages;
    }

    private boolean isReachableFrom(ClassLoader classLoader, ClassLoader origin) {
        if (classLoader == origin) {
            return true;
        }
        if (origin.getParent() != null) {
            return isReachableFrom(classLoader, origin.getParent());
        }
        return false;
    }

    @Nullable
    private static String getPackageName(String className) {
        var lastPkgSepIdx = className.lastIndexOf('/');
        if (lastPkgSepIdx != -1) {
            return className.substring(0, lastPkgSepIdx);
        }
        return null;
    }
}
