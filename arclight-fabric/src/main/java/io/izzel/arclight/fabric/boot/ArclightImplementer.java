package io.izzel.arclight.fabric.boot;

import io.izzel.arclight.boot.asm.*;
import net.fabricmc.loader.impl.game.patch.GameTransformer;
import net.fabricmc.loader.impl.launch.FabricLauncher;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ArclightImplementer extends GameTransformer {

    private final GameTransformer delegate;
    private final MethodHandle getRawClassByteArray;

    private final Map<String, Implementer> implementers = new HashMap<>();

    public ArclightImplementer(GameTransformer delegate, FabricLauncher launcher) throws Exception {
        this.delegate = delegate;
        Field classLoaderField = launcher.getClass().getDeclaredField("classLoader");
        classLoaderField.setAccessible(true);
        Object knotClassLoader = classLoaderField.get(launcher);
        Method getRawClassByteArrayMethod = knotClassLoader.getClass().getDeclaredMethod("getRawClassByteArray", String.class, boolean.class);
        getRawClassByteArrayMethod.setAccessible(true);
        this.getRawClassByteArray = MethodHandles.lookup().unreflect(getRawClassByteArrayMethod).bindTo(knotClassLoader);
        this.implementers.put("inventory", new InventoryImplementer());
        this.implementers.put("switch", SwitchTableFixer.INSTANCE);
        this.implementers.put("async", AsyncCatcher.INSTANCE);
        this.implementers.put("enum", new EnumDefinalizer());
        boolean logger = detectTransformLogger();
        if (logger) {
            this.implementers.put("logger", new LoggerTransformer());
        }
    }

    @Override
    public void locateEntrypoints(FabricLauncher launcher, List<Path> gameJars) {
        delegate.locateEntrypoints(launcher, gameJars);
    }

    @Override
    public byte[] transform(String className) {
        byte[] classBytes = delegate.transform(className);
        if (classBytes == null) {
            try {
                classBytes = (byte[]) this.getRawClassByteArray.invokeExact(className, false);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }
        if (classBytes != null) {
            var reader = new ClassReader(classBytes);
            var node = new ClassNode();
            reader.accept(node, 0);
            for (var implementerEntry : implementers.entrySet()) {
                var implementer = implementerEntry.getValue();
                implementer.processClass(node);
            }
            var classWriter = new ClassWriter(0);
            node.accept(classWriter);
            classBytes = classWriter.toByteArray();
        }
        return classBytes;
    }

    private static boolean detectTransformLogger() {
        var transformLogger = !(java.util.logging.LogManager.getLogManager().getClass().getName().equals("org.apache.logging.log4j.jul.LogManager"));
        if (transformLogger && !System.getProperties().contains("log4j.jul.LoggerAdapter")) {
            System.setProperty("log4j.jul.LoggerAdapter", "io.izzel.arclight.boot.log.ArclightLoggerAdapter");
        }
        return transformLogger;
    }
}
