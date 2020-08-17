package io.izzel.arclight.common.mixin.bukkit;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.izzel.arclight.api.Unsafe;
import io.izzel.arclight.common.bridge.bukkit.JavaPluginLoaderBridge;
import org.apache.commons.lang3.Validate;
import org.bukkit.Server;
import org.bukkit.Warning;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.AuthorNagException;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredListener;
import org.bukkit.plugin.java.JavaPluginLoader;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

@Mixin(value = JavaPluginLoader.class, remap = false)
public abstract class JavaPluginLoaderMixin implements JavaPluginLoaderBridge {

    // @formatter:off
    @Shadow @Final Server server;
    @Invoker("getClassByName") public abstract Class<?> bridge$getClassByName(final String name);
    @Invoker("setClass")  public abstract void bridge$setClass(final String name, final Class<?> clazz);
    @Accessor("loaders") public abstract List<URLClassLoader> bridge$getLoaders();
    // @formatter:on

    private static final AtomicInteger COUNTER = new AtomicInteger();
    private static final Cache<Method, Class<? extends EventExecutor>> EXECUTOR_CACHE = CacheBuilder.newBuilder().build();

    /**
     * @author IzzelAliz
     * @reason use asm event executor
     */
    @Overwrite
    @NotNull
    public Map<Class<? extends Event>, Set<RegisteredListener>> createRegisteredListeners(@NotNull Listener listener, @NotNull Plugin plugin) {
        Validate.notNull(plugin, "Plugin can not be null");
        Validate.notNull(listener, "Listener can not be null");

        boolean useTimings = server.getPluginManager().useTimings();
        Map<Class<? extends Event>, Set<RegisteredListener>> ret = new HashMap<>();
        Set<Method> methods;
        try {
            Method[] publicMethods = listener.getClass().getMethods();
            Method[] privateMethods = listener.getClass().getDeclaredMethods();
            methods = new HashSet<>(publicMethods.length + privateMethods.length, 1.0f);
            methods.addAll(Arrays.asList(publicMethods));
            methods.addAll(Arrays.asList(privateMethods));
        } catch (NoClassDefFoundError e) {
            plugin.getLogger().severe("Plugin " + plugin.getDescription().getFullName() + " has failed to register events for " + listener.getClass() + " because " + e.getMessage() + " does not exist.");
            return ret;
        }

        for (final Method method : methods) {
            final EventHandler eh = method.getAnnotation(EventHandler.class);
            if (eh == null) continue;
            // Do not register bridge or synthetic methods to avoid event duplication
            // Fixes SPIGOT-893
            if (method.isBridge() || method.isSynthetic()) {
                continue;
            }
            final Class<?> checkClass;
            if (method.getParameterTypes().length != 1 || !Event.class.isAssignableFrom(checkClass = method.getParameterTypes()[0])) {
                plugin.getLogger().severe(plugin.getDescription().getFullName() + " attempted to register an invalid EventHandler method signature \"" + method.toGenericString() + "\" in " + listener.getClass());
                continue;
            }
            final Class<? extends Event> eventClass = checkClass.asSubclass(Event.class);
            method.setAccessible(true);
            Set<RegisteredListener> eventSet = ret.get(eventClass);
            if (eventSet == null) {
                eventSet = new HashSet<>();
                ret.put(eventClass, eventSet);
            }

            for (Class<?> clazz = eventClass; Event.class.isAssignableFrom(clazz); clazz = clazz.getSuperclass()) {
                // This loop checks for extending deprecated events
                if (clazz.getAnnotation(Deprecated.class) != null) {
                    Warning warning = clazz.getAnnotation(Warning.class);
                    Warning.WarningState warningState = server.getWarningState();
                    if (!warningState.printFor(warning)) {
                        break;
                    }
                    plugin.getLogger().log(
                        Level.WARNING,
                        String.format(
                            "\"%s\" has registered a listener for %s on method \"%s\", but the event is Deprecated. \"%s\"; please notify the authors %s.",
                            plugin.getDescription().getFullName(),
                            clazz.getName(),
                            method.toGenericString(),
                            (warning != null && warning.reason().length() != 0) ? warning.reason() : "Server performance will be affected",
                            Arrays.toString(plugin.getDescription().getAuthors().toArray())),
                        warningState == Warning.WarningState.ON ? new AuthorNagException(null) : null);
                    break;
                }
            }

            // final CustomTimingsHandler timings = new CustomTimingsHandler("Plugin: " + plugin.getDescription().getFullName() + " Event: " + listener.getClass().getName() + "::" + method.getName() + "(" + eventClass.getSimpleName() + ")", pluginParentTimer); // Spigot

            try {
                Class<? extends EventExecutor> executorClass = createExecutor(method, eventClass);
                Constructor<? extends EventExecutor> constructor = executorClass.getDeclaredConstructor();
                constructor.setAccessible(true);
                EventExecutor executor = constructor.newInstance();
                eventSet.add(new RegisteredListener(listener, executor, eh.priority(), plugin, eh.ignoreCancelled()));
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
        return ret;
    }

    @SuppressWarnings("unchecked")
    private Class<? extends EventExecutor> createExecutor(Method method, Class<? extends Event> eventClass) throws ExecutionException {
        return EXECUTOR_CACHE.get(method, () -> {
            ClassWriter cv = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            cv.visit(Opcodes.V1_8,
                Opcodes.ACC_SUPER + Opcodes.ACC_SYNTHETIC + Opcodes.ACC_FINAL,
                Type.getInternalName(method.getDeclaringClass()) + "$$arclight$" + COUNTER.getAndIncrement(),
                null,
                Type.getInternalName(Object.class),
                new String[]{Type.getInternalName(EventExecutor.class)}
            );
            cv.visitOuterClass(Type.getInternalName(method.getDeclaringClass()), null, null);
            createConstructor(cv);
            createImpl(method, eventClass, cv);
            cv.visitEnd();
            return (Class<? extends EventExecutor>) Unsafe.defineAnonymousClass(method.getDeclaringClass(), cv.toByteArray(), null);
        });
    }

    private void createConstructor(ClassVisitor cv) {
        MethodVisitor mv = cv.visitMethod(
            Opcodes.ACC_PRIVATE,
            "<init>",
            "()V",
            null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, Type.getInternalName(Object.class), "<init>", "()V", false);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(-1, -1);
        mv.visitEnd();
    }

    private void createImpl(Method method, Class<? extends Event> eventClass, ClassVisitor cv) {
        String ownerType = Type.getInternalName(method.getDeclaringClass());
        MethodVisitor mv = cv.visitMethod(
            Opcodes.ACC_PUBLIC,
            "execute",
            Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(Listener.class), Type.getType(Event.class)),
            null, null
        );
        mv.visitAnnotation("Ljava/lang/invoke/LambdaForm$Hidden;", true);

        Label label0 = new Label();
        Label label1 = new Label();
        Label label2 = new Label();
        mv.visitTryCatchBlock(label0, label1, label2, "java/lang/Throwable");
        Label label3 = new Label();
        Label label4 = new Label();
        // try {
        mv.visitTryCatchBlock(label3, label4, label2, "java/lang/Throwable");
        //   if (!(event instanceof TYPE))
        mv.visitLabel(label0);
        mv.visitVarInsn(Opcodes.ALOAD, 2);
        mv.visitTypeInsn(Opcodes.INSTANCEOF, Type.getInternalName(eventClass));
        mv.visitJumpInsn(Opcodes.IFNE, label3);
        //      return;
        mv.visitLabel(label1);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitLabel(label3);
        mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
        //   ((TYPE) listener).<method>(event);
        //   TYPE.<method>(event);
        int invokeCode;
        if (Modifier.isStatic(method.getModifiers())) {
            invokeCode = Opcodes.INVOKESTATIC;
        } else if (method.getDeclaringClass().isInterface()) {
            invokeCode = Opcodes.INVOKEINTERFACE;
        } else if (Modifier.isPrivate(method.getModifiers())) {
            invokeCode = Opcodes.INVOKESPECIAL;
        } else {
            invokeCode = Opcodes.INVOKEVIRTUAL;
        }
        if (invokeCode != Opcodes.INVOKESTATIC) {
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitTypeInsn(Opcodes.CHECKCAST, ownerType);
        }
        mv.visitVarInsn(Opcodes.ALOAD, 2);
        mv.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(eventClass));
        mv.visitMethodInsn(invokeCode, ownerType, method.getName(), Type.getMethodDescriptor(method), invokeCode == Opcodes.INVOKEINTERFACE);
        int retSize = Type.getType(method.getReturnType()).getSize();
        if (retSize > 0) {
            mv.visitInsn(Opcodes.POP + retSize - 1);
        }
        mv.visitLabel(label4);
        // } catch (Throwable t) {
        Label label5 = new Label();
        mv.visitJumpInsn(Opcodes.GOTO, label5);
        mv.visitLabel(label2);
        mv.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[]{"java/lang/Throwable"});
        mv.visitVarInsn(Opcodes.ASTORE, 3);
        // throw new EventException(t);
        Label label6 = new Label();
        mv.visitLabel(label6);
        mv.visitTypeInsn(Opcodes.NEW, "org/bukkit/event/EventException");
        mv.visitInsn(Opcodes.DUP);
        mv.visitVarInsn(Opcodes.ALOAD, 3);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "org/bukkit/event/EventException", "<init>", "(Ljava/lang/Throwable;)V", false);
        mv.visitInsn(Opcodes.ATHROW);
        mv.visitLabel(label5);
        mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
        mv.visitInsn(Opcodes.RETURN);
        // }
        Label label7 = new Label();
        mv.visitLabel(label7);
        mv.visitMaxs(-1, -1);
        mv.visitEnd();
    }
}
