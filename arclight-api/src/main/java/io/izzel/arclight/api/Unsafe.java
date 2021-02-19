package io.izzel.arclight.api;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.ProtectionDomain;
import java.util.Objects;

@SuppressWarnings("all")
public class Unsafe {

    private static final sun.misc.Unsafe unsafe;
    private static final MethodHandles.Lookup lookup;
    private static final MethodHandle defineClass;

    static {
        try {
            Field theUnsafe = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            unsafe = (sun.misc.Unsafe) theUnsafe.get(null);
            Unsafe.ensureClassInitialized(MethodHandles.Lookup.class);
            Field field = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
            Object base = unsafe.staticFieldBase(field);
            long offset = unsafe.staticFieldOffset(field);
            lookup = (MethodHandles.Lookup) unsafe.getObject(base, offset);
            MethodHandle mh;
            try {
                Method sunMisc = unsafe.getClass().getMethod("defineClass", String.class, byte[].class, int.class, int.class, ClassLoader.class, ProtectionDomain.class);
                mh = lookup.unreflect(sunMisc).bindTo(unsafe);
            } catch (Exception e) {
                Class<?> jdkInternalUnsafe = Class.forName("jdk.internal.misc.Unsafe");
                Field internalUnsafeField = jdkInternalUnsafe.getDeclaredField("theUnsafe");
                Object internalUnsafe = unsafe.getObject(unsafe.staticFieldBase(internalUnsafeField), unsafe.staticFieldOffset(internalUnsafeField));
                Method internalDefineClass = jdkInternalUnsafe.getMethod("defineClass", String.class, byte[].class, int.class, int.class, ClassLoader.class, ProtectionDomain.class);
                mh = lookup.unreflect(internalDefineClass).bindTo(internalUnsafe);
            }
            defineClass = Objects.requireNonNull(mh);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T getStatic(Class<?> cl, String name) {
        try {
            Unsafe.ensureClassInitialized(cl);
            Field field = cl.getDeclaredField(name);
            Object materialByNameBase = Unsafe.staticFieldBase(field);
            long materialByNameOffset = Unsafe.staticFieldOffset(field);
            return (T) Unsafe.getObject(materialByNameBase, materialByNameOffset);
        } catch (Exception e) {
            return null;
        }
    }

    public static MethodHandles.Lookup lookup() {
        return lookup;
    }

    public static sun.misc.Unsafe getUnsafe() {
        return unsafe;
    }

    public static int getInt(Object o, long l) {
        return unsafe.getInt(o, l);
    }

    public static void putInt(Object o, long l, int i) {
        unsafe.putInt(o, l, i);
    }

    public static Object getObject(Object o, long l) {
        return unsafe.getObject(o, l);
    }

    public static void putObject(Object o, long l, Object o1) {
        unsafe.putObject(o, l, o1);
    }

    public static boolean getBoolean(Object o, long l) {
        return unsafe.getBoolean(o, l);
    }

    public static void putBoolean(Object o, long l, boolean b) {
        unsafe.putBoolean(o, l, b);
    }

    public static byte getByte(Object o, long l) {
        return unsafe.getByte(o, l);
    }

    public static void putByte(Object o, long l, byte b) {
        unsafe.putByte(o, l, b);
    }

    public static short getShort(Object o, long l) {
        return unsafe.getShort(o, l);
    }

    public static void putShort(Object o, long l, short i) {
        unsafe.putShort(o, l, i);
    }

    public static char getChar(Object o, long l) {
        return unsafe.getChar(o, l);
    }

    public static void putChar(Object o, long l, char c) {
        unsafe.putChar(o, l, c);
    }

    public static long getLong(Object o, long l) {
        return unsafe.getLong(o, l);
    }

    public static void putLong(Object o, long l, long l1) {
        unsafe.putLong(o, l, l1);
    }

    public static float getFloat(Object o, long l) {
        return unsafe.getFloat(o, l);
    }

    public static void putFloat(Object o, long l, float v) {
        unsafe.putFloat(o, l, v);
    }

    public static double getDouble(Object o, long l) {
        return unsafe.getDouble(o, l);
    }

    public static void putDouble(Object o, long l, double v) {
        unsafe.putDouble(o, l, v);
    }

    public static byte getByte(long l) {
        return unsafe.getByte(l);
    }

    public static void putByte(long l, byte b) {
        unsafe.putByte(l, b);
    }

    public static short getShort(long l) {
        return unsafe.getShort(l);
    }

    public static void putShort(long l, short i) {
        unsafe.putShort(l, i);
    }

    public static char getChar(long l) {
        return unsafe.getChar(l);
    }

    public static void putChar(long l, char c) {
        unsafe.putChar(l, c);
    }

    public static int getInt(long l) {
        return unsafe.getInt(l);
    }

    public static void putInt(long l, int i) {
        unsafe.putInt(l, i);
    }

    public static long getLong(long l) {
        return unsafe.getLong(l);
    }

    public static void putLong(long l, long l1) {
        unsafe.putLong(l, l1);
    }

    public static float getFloat(long l) {
        return unsafe.getFloat(l);
    }

    public static void putFloat(long l, float v) {
        unsafe.putFloat(l, v);
    }

    public static double getDouble(long l) {
        return unsafe.getDouble(l);
    }

    public static void putDouble(long l, double v) {
        unsafe.putDouble(l, v);
    }

    public static long getAddress(long l) {
        return unsafe.getAddress(l);
    }

    public static void putAddress(long l, long l1) {
        unsafe.putAddress(l, l1);
    }

    public static long allocateMemory(long l) {
        return unsafe.allocateMemory(l);
    }

    public static long reallocateMemory(long l, long l1) {
        return unsafe.reallocateMemory(l, l1);
    }

    public static void setMemory(Object o, long l, long l1, byte b) {
        unsafe.setMemory(o, l, l1, b);
    }

    public static void setMemory(long l, long l1, byte b) {
        unsafe.setMemory(l, l1, b);
    }

    public static void copyMemory(Object o, long l, Object o1, long l1, long l2) {
        unsafe.copyMemory(o, l, o1, l1, l2);
    }

    public static void copyMemory(long l, long l1, long l2) {
        unsafe.copyMemory(l, l1, l2);
    }

    public static void freeMemory(long l) {
        unsafe.freeMemory(l);
    }

    public static long staticFieldOffset(Field field) {
        return unsafe.staticFieldOffset(field);
    }

    public static long objectFieldOffset(Field field) {
        return unsafe.objectFieldOffset(field);
    }

    public static Object staticFieldBase(Field field) {
        return unsafe.staticFieldBase(field);
    }

    public static boolean shouldBeInitialized(Class<?> aClass) {
        return unsafe.shouldBeInitialized(aClass);
    }

    public static void ensureClassInitialized(Class<?> aClass) {
        unsafe.ensureClassInitialized(aClass);
    }

    public static int arrayBaseOffset(Class<?> aClass) {
        return unsafe.arrayBaseOffset(aClass);
    }

    public static int arrayIndexScale(Class<?> aClass) {
        return unsafe.arrayIndexScale(aClass);
    }

    public static int addressSize() {
        return unsafe.addressSize();
    }

    public static int pageSize() {
        return unsafe.pageSize();
    }

    public static Class<?> defineClass(String s, byte[] bytes, int i, int i1, ClassLoader classLoader, ProtectionDomain protectionDomain) {
        try {
            return (Class<?>) defineClass.invokeExact(s, bytes, i, i1, classLoader, protectionDomain);
        } catch (Throwable throwable) {
            throwException(throwable);
            return null;
        }
    }

    public static Class<?> defineAnonymousClass(Class<?> aClass, byte[] bytes, Object[] objects) {
        return unsafe.defineAnonymousClass(aClass, bytes, objects);
    }

    public static Object allocateInstance(Class<?> aClass) throws InstantiationException {
        return unsafe.allocateInstance(aClass);
    }

    public static void throwException(Throwable throwable) {
        unsafe.throwException(throwable);
    }

    public static boolean compareAndSwapObject(Object o, long l, Object o1, Object o2) {
        return unsafe.compareAndSwapObject(o, l, o1, o2);
    }

    public static boolean compareAndSwapInt(Object o, long l, int i, int i1) {
        return unsafe.compareAndSwapInt(o, l, i, i1);
    }

    public static boolean compareAndSwapLong(Object o, long l, long l1, long l2) {
        return unsafe.compareAndSwapLong(o, l, l1, l2);
    }

    public static Object getObjectVolatile(Object o, long l) {
        return unsafe.getObjectVolatile(o, l);
    }

    public static void putObjectVolatile(Object o, long l, Object o1) {
        unsafe.putObjectVolatile(o, l, o1);
    }

    public static int getIntVolatile(Object o, long l) {
        return unsafe.getIntVolatile(o, l);
    }

    public static void putIntVolatile(Object o, long l, int i) {
        unsafe.putIntVolatile(o, l, i);
    }

    public static boolean getBooleanVolatile(Object o, long l) {
        return unsafe.getBooleanVolatile(o, l);
    }

    public static void putBooleanVolatile(Object o, long l, boolean b) {
        unsafe.putBooleanVolatile(o, l, b);
    }

    public static byte getByteVolatile(Object o, long l) {
        return unsafe.getByteVolatile(o, l);
    }

    public static void putByteVolatile(Object o, long l, byte b) {
        unsafe.putByteVolatile(o, l, b);
    }

    public static short getShortVolatile(Object o, long l) {
        return unsafe.getShortVolatile(o, l);
    }

    public static void putShortVolatile(Object o, long l, short i) {
        unsafe.putShortVolatile(o, l, i);
    }

    public static char getCharVolatile(Object o, long l) {
        return unsafe.getCharVolatile(o, l);
    }

    public static void putCharVolatile(Object o, long l, char c) {
        unsafe.putCharVolatile(o, l, c);
    }

    public static long getLongVolatile(Object o, long l) {
        return unsafe.getLongVolatile(o, l);
    }

    public static void putLongVolatile(Object o, long l, long l1) {
        unsafe.putLongVolatile(o, l, l1);
    }

    public static float getFloatVolatile(Object o, long l) {
        return unsafe.getFloatVolatile(o, l);
    }

    public static void putFloatVolatile(Object o, long l, float v) {
        unsafe.putFloatVolatile(o, l, v);
    }

    public static double getDoubleVolatile(Object o, long l) {
        return unsafe.getDoubleVolatile(o, l);
    }

    public static void putDoubleVolatile(Object o, long l, double v) {
        unsafe.putDoubleVolatile(o, l, v);
    }

    public static void putOrderedObject(Object o, long l, Object o1) {
        unsafe.putOrderedObject(o, l, o1);
    }

    public static void putOrderedInt(Object o, long l, int i) {
        unsafe.putOrderedInt(o, l, i);
    }

    public static void putOrderedLong(Object o, long l, long l1) {
        unsafe.putOrderedLong(o, l, l1);
    }

    public static void unpark(Object o) {
        unsafe.unpark(o);
    }

    public static void park(boolean b, long l) {
        unsafe.park(b, l);
    }

    public static int getLoadAverage(double[] doubles, int i) {
        return unsafe.getLoadAverage(doubles, i);
    }

    public static int getAndAddInt(Object o, long l, int i) {
        return unsafe.getAndAddInt(o, l, i);
    }

    public static long getAndAddLong(Object o, long l, long l1) {
        return unsafe.getAndAddLong(o, l, l1);
    }

    public static int getAndSetInt(Object o, long l, int i) {
        return unsafe.getAndSetInt(o, l, i);
    }

    public static long getAndSetLong(Object o, long l, long l1) {
        return unsafe.getAndSetLong(o, l, l1);
    }

    public static Object getAndSetObject(Object o, long l, Object o1) {
        return unsafe.getAndSetObject(o, l, o1);
    }

    public static void loadFence() {
        unsafe.loadFence();
    }

    public static void storeFence() {
        unsafe.storeFence();
    }

    public static void fullFence() {
        unsafe.fullFence();
    }

    public static Class<?> getCallerClass() {
        return INSTANCE.getClassContext()[3];
    }

    private static final CallerClass INSTANCE = new CallerClass();

    private static class CallerClass extends SecurityManager {

        @Override
        public Class<?>[] getClassContext() {
            return super.getClassContext();
        }
    }
}
