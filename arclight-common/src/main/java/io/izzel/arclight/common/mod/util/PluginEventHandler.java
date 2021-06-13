package io.izzel.arclight.common.mod.util;

import io.izzel.arclight.api.Unsafe;
import io.izzel.arclight.common.mod.ArclightMod;
import net.minecraftforge.eventbus.ASMEventHandler;
import net.minecraftforge.eventbus.EventBus;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventListener;
import net.minecraftforge.eventbus.api.IGenericEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.bukkit.plugin.Plugin;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.objectweb.asm.Opcodes.*;

public class PluginEventHandler extends ASMEventHandler {

    private static final String HANDLER_DESC = Type.getInternalName(IEventListener.class);
    private static final String HANDLER_FUNC_DESC = Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(Event.class));
    private static final MethodHandle MH_GET_LISTENERS;
    private static final MethodHandle MH_ADD_LISTENERS;
    private static final MethodHandle MH_UNIQUE_NAME;

    static {
        try {
            MH_GET_LISTENERS = Unsafe.lookup().findGetter(EventBus.class, "listeners", ConcurrentHashMap.class);
            MH_ADD_LISTENERS = Unsafe.lookup().findVirtual(EventBus.class, "addToListeners", MethodType.methodType(void.class,
                Object.class, Class.class, IEventListener.class, EventPriority.class));
            MH_UNIQUE_NAME = Unsafe.lookup().findVirtual(ASMEventHandler.class, "getUniqueName", MethodType.methodType(String.class, Method.class));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private final Plugin plugin;

    public PluginEventHandler(Plugin plugin, Object target, Method method, boolean isGeneric) throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
        super(target, method, isGeneric);
        this.plugin = plugin;
    }

    @SuppressWarnings("unchecked")
    public static void register(Plugin plugin, EventBus bus, Object target) throws Throwable {
        ConcurrentHashMap<Object, ?> listeners = (ConcurrentHashMap<Object, ?>) MH_GET_LISTENERS.invokeExact(bus);
        if (!listeners.containsKey(target)) {
            if (target.getClass() == Class.class) {
                registerClass((Class<?>) target, plugin, bus);
            } else {
                registerObject(target, plugin, bus);
            }
        }
    }

    private static void registerClass(final Class<?> clazz, Plugin plugin, EventBus bus) {
        Arrays.stream(clazz.getMethods()).
            filter(m -> Modifier.isStatic(m.getModifiers())).
            filter(m -> m.isAnnotationPresent(SubscribeEvent.class)).
            forEach(m -> registerListener(clazz, m, m, plugin, bus));
    }

    private static Optional<Method> getDeclMethod(final Class<?> clz, final Method in) {
        try {
            return Optional.of(clz.getDeclaredMethod(in.getName(), in.getParameterTypes()));
        } catch (NoSuchMethodException nse) {
            return Optional.empty();
        }

    }

    private static void registerObject(final Object obj, Plugin plugin, EventBus bus) {
        final HashSet<Class<?>> classes = new HashSet<>();
        typesFor(obj.getClass(), classes);
        Arrays.stream(obj.getClass().getMethods()).
            filter(m -> !Modifier.isStatic(m.getModifiers())).
            forEach(m -> classes.stream().
                map(c -> getDeclMethod(c, m)).
                filter(rm -> rm.isPresent() && rm.get().isAnnotationPresent(SubscribeEvent.class)).
                findFirst().
                ifPresent(rm -> registerListener(obj, m, rm.get(), plugin, bus)));
    }

    private static void typesFor(final Class<?> clz, final Set<Class<?>> visited) {
        if (clz.getSuperclass() == null) return;
        typesFor(clz.getSuperclass(), visited);
        Arrays.stream(clz.getInterfaces()).forEach(i -> typesFor(i, visited));
        visited.add(clz);
    }

    private static void registerListener(final Object target, final Method method, final Method real, Plugin plugin, EventBus bus) {
        Class<?>[] parameterTypes = method.getParameterTypes();
        if (parameterTypes.length != 1) {
            throw new IllegalArgumentException(
                "Method " + method + " has @SubscribeEvent annotation. " +
                    "It has " + parameterTypes.length + " arguments, " +
                    "but event handler methods require a single argument only."
            );
        }

        Class<?> eventType = parameterTypes[0];

        if (!Event.class.isAssignableFrom(eventType)) {
            throw new IllegalArgumentException(
                "Method " + method + " has @SubscribeEvent annotation, " +
                    "but takes an argument that is not an Event subtype : " + eventType);
        }

        register(eventType, target, real, plugin, bus);
    }

    private static void register(Class<?> eventType, Object target, Method method, Plugin plugin, EventBus bus) {
        try {
            ASMEventHandler asm = new PluginEventHandler(plugin, target, method, IGenericEvent.class.isAssignableFrom(eventType));
            MH_ADD_LISTENERS.invokeExact(bus, target, eventType, (IEventListener) asm, asm.getPriority());
        } catch (Throwable e) {
            ArclightMod.LOGGER.error("Error registering event handler: {} {}", eventType, method, e);
        }
    }

    @Override
    public Class<?> createWrapper(Method callback) {

        ClassWriter cw = new ClassWriter(0);
        MethodVisitor mv;

        boolean isStatic = Modifier.isStatic(callback.getModifiers());
        String name;
        try {
            name = (String) MH_UNIQUE_NAME.invoke(this, callback);
        } catch (Throwable throwable) {
            throw new RuntimeException(throwable);
        }
        String desc = name.replace('.', '/');
        String instType = Type.getInternalName(callback.getDeclaringClass());
        String eventType = Type.getInternalName(callback.getParameterTypes()[0]);

        cw.visit(V1_6, ACC_PUBLIC | ACC_SUPER, desc, null, "java/lang/Object", new String[]{HANDLER_DESC});

        cw.visitSource(".dynamic", null);
        {
            if (!isStatic)
                cw.visitField(ACC_PUBLIC, "instance", "Ljava/lang/Object;", null, null).visitEnd();
        }
        {
            mv = cw.visitMethod(ACC_PUBLIC, "<init>", isStatic ? "()V" : "(Ljava/lang/Object;)V", null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
            if (!isStatic) {
                mv.visitVarInsn(ALOAD, 0);
                mv.visitVarInsn(ALOAD, 1);
                mv.visitFieldInsn(PUTFIELD, desc, "instance", "Ljava/lang/Object;");
            }
            mv.visitInsn(RETURN);
            mv.visitMaxs(2, 2);
            mv.visitEnd();
        }
        {
            mv = cw.visitMethod(ACC_PUBLIC, "invoke", HANDLER_FUNC_DESC, null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            if (!isStatic) {
                mv.visitFieldInsn(GETFIELD, desc, "instance", "Ljava/lang/Object;");
                mv.visitTypeInsn(CHECKCAST, instType);
            }
            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, eventType);
            mv.visitMethodInsn(isStatic ? INVOKESTATIC : INVOKEVIRTUAL, instType, callback.getName(), Type.getMethodDescriptor(callback), false);
            mv.visitInsn(RETURN);
            mv.visitMaxs(2, 2);
            mv.visitEnd();
        }
        cw.visitEnd();
        byte[] bytes = cw.toByteArray();
        return Unsafe.defineClass(name, bytes, 0, bytes.length, plugin.getClass().getClassLoader(), plugin.getClass().getProtectionDomain());
    }

    @Override
    public String toString() {
        return "PL:" + plugin.getName() + " " + super.toString();
    }
}
