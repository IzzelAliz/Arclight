package io.izzel.arclight.forge.mod.util;

import io.izzel.arclight.api.Unsafe;
import io.izzel.arclight.common.mod.server.ArclightServer;
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
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.objectweb.asm.Opcodes.*;

public class PluginEventHandler implements IEventListener {

    private static final String HANDLER_DESC = Type.getInternalName(IEventListener.class);
    private static final String HANDLER_FUNC_DESC = Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(Event.class));
    private static final MethodHandle MH_GET_LISTENERS;
    private static final MethodHandle MH_ADD_LISTENERS;

    static {
        try {
            MH_GET_LISTENERS = Unsafe.lookup().findGetter(EventBus.class, "listeners", ConcurrentHashMap.class);
            MH_ADD_LISTENERS = Unsafe.lookup().findVirtual(EventBus.class, "addToListeners", MethodType.methodType(void.class,
                Object.class, Class.class, IEventListener.class, EventPriority.class));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private final IEventListener handler;
    private final SubscribeEvent subInfo;
    private java.lang.reflect.Type filter = null;
    private String readable;


    public PluginEventHandler(Plugin plugin, Object target, Method method, boolean isGeneric) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        if (Modifier.isStatic(method.getModifiers())) {
            handler = (IEventListener) createWrapper(method).getDeclaredConstructor().newInstance();
        } else {
            handler = (IEventListener) createWrapper(method).getConstructor(Object.class).newInstance(target);
        }
        subInfo = method.getAnnotation(SubscribeEvent.class);
        readable = "PL: " + plugin.getName() + " ASM: " + target + " " + method.getName() + Type.getMethodDescriptor(method);
        if (isGeneric) {
            java.lang.reflect.Type type = method.getGenericParameterTypes()[0];
            if (type instanceof ParameterizedType) {
                filter = ((ParameterizedType) type).getActualTypeArguments()[0];
                if (filter instanceof ParameterizedType) // Unlikely that nested generics will ever be relevant for event filtering, so discard them
                {
                    filter = ((ParameterizedType) filter).getRawType();
                } else if (filter instanceof WildcardType wfilter) {
                    // If there's a wildcard filter of Object.class, then remove the filter.
                    if (wfilter.getUpperBounds().length == 1 && wfilter.getUpperBounds()[0] == Object.class && wfilter.getLowerBounds().length == 0) {
                        filter = null;
                    }
                }
            }
        }
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
            var asm = new PluginEventHandler(plugin, target, method, IGenericEvent.class.isAssignableFrom(eventType));
            MH_ADD_LISTENERS.invokeExact(bus, target, eventType, (IEventListener) asm, asm.getPriority());
        } catch (Throwable e) {
            ArclightServer.LOGGER.error("Error registering event handler: {} {}", eventType, method, e);
        }
    }

    private String getUniqueName(Method callback) {
        return String.format("%s.__%s_%s_%s", callback.getDeclaringClass().getPackageName(), callback.getDeclaringClass().getSimpleName(),
            callback.getName(),
            callback.getParameterTypes()[0].getSimpleName());
    }

    public Class<?> createWrapper(Method callback) {

        ClassWriter cw = new ClassWriter(0);
        MethodVisitor mv;

        boolean isStatic = Modifier.isStatic(callback.getModifiers());
        String name = getUniqueName(callback);
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
        return Unsafe.defineClass(name, bytes, 0, bytes.length, callback.getDeclaringClass().getClassLoader(), callback.getDeclaringClass().getProtectionDomain());
    }

    public EventPriority getPriority() {
        return subInfo.priority();
    }

    @SuppressWarnings("rawtypes")
    @Override
    public void invoke(Event event) {
        if (handler != null) {
            if (!event.isCancelable() || !event.isCanceled() || subInfo.receiveCanceled()) {
                if (filter == null || filter == ((IGenericEvent) event).getGenericType()) {
                    handler.invoke(event);
                }
            }
        }
    }

    @Override
    public String toString() {
        return readable;
    }
}
