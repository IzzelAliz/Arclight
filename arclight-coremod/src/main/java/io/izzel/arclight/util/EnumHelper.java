package io.izzel.arclight.util;

import com.google.common.collect.ImmutableList;
import io.izzel.arclight.api.Unsafe;
import org.bukkit.Material;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

public class EnumHelper {

    private static Map<String, Material> BY_NAME;

    @SuppressWarnings("unchecked")
    public static Material addMaterial(String name, final int id, final int stack, final int durability) {
        if (BY_NAME == null) {
            try {
                Unsafe.ensureClassInitialized(Material.class);
                Field field = Material.class.getDeclaredField("BY_NAME");
                Object materialByNameBase = Unsafe.staticFieldBase(field);
                long materialByNameOffset = Unsafe.staticFieldOffset(field);
                BY_NAME = (Map<String, Material>) Unsafe.getObject(materialByNameBase, materialByNameOffset);
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            }
        }
        Material material = addEnum(
            Material.class, name,
            ImmutableList.of(int.class, int.class, int.class),
            ImmutableList.of(id, stack, durability)
        );
        BY_NAME.put(name, material);
        return material;
    }

    @SuppressWarnings("unchecked")
    public static <T> T addEnum(Class<T> cl, String name, List<Class<?>> ctorTypes, List<Object> ctorParams) {
        try {
            Unsafe.ensureClassInitialized(cl);
            Field field = cl.getDeclaredField("ENUM$VALUES");
            Object base = Unsafe.staticFieldBase(field);
            long offset = Unsafe.staticFieldOffset(field);
            T[] arr = (T[]) Unsafe.getObject(base, offset);
            T[] newArr = (T[]) Array.newInstance(cl, arr.length + 1);
            System.arraycopy(arr, 0, newArr, 0, arr.length);

            List<Class<?>> ctor = ImmutableList.<Class<?>>builder().add(String.class, int.class).addAll(ctorTypes).build();
            MethodHandle constructor = Unsafe.lookup().findConstructor(cl, MethodType.methodType(void.class, ctor));
            List<Object> param = ImmutableList.builder().add(name, arr.length).addAll(ctorParams).build();
            T newInstance = (T) constructor.invokeWithArguments(param);

            newArr[arr.length] = newInstance;
            Unsafe.putObject(base, offset, newArr);
            reset(cl);
            return newInstance;
        } catch (Throwable e) {
            e.printStackTrace();
            return null;
        }
    }

    public static <T> void addEnums(Class<T> cl, List<T> list) {
        try {
            Field field = cl.getDeclaredField("ENUM$VALUES");
            Object base = Unsafe.staticFieldBase(field);
            long offset = Unsafe.staticFieldOffset(field);
            T[] arr = (T[]) Unsafe.getObject(base, offset);
            T[] newArr = (T[]) Array.newInstance(cl, arr.length + list.size());
            System.arraycopy(arr, 0, newArr, 0, arr.length);
            for (int i = 0; i < list.size(); i++) {
                newArr[arr.length + i] = list.get(i);
            }
            Unsafe.putObject(base, offset, newArr);
            reset(cl);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T makeEnum(Class<T> cl, String name, int i, List<Class<?>> ctorTypes, List<Object> ctorParams) {
        try {
            Unsafe.ensureClassInitialized(cl);
            List<Class<?>> ctor = ImmutableList.<Class<?>>builder().add(String.class, int.class).addAll(ctorTypes).build();
            MethodHandle constructor = Unsafe.lookup().findConstructor(cl, MethodType.methodType(void.class, ctor));
            List<Object> param = ImmutableList.builder().add(name, i).addAll(ctorParams).build();
            return (T) constructor.invokeWithArguments(param);
        } catch (Throwable e) {
            e.printStackTrace();
            return null;
        }
    }

    private static long enumConstantDirectoryOffset;
    private static long enumConstantsOffset;

    static {
        try {
            Field enumConstantDirectory = Class.class.getDeclaredField("enumConstantDirectory");
            Field enumConstants = Class.class.getDeclaredField("enumConstants");
            enumConstantDirectoryOffset = Unsafe.objectFieldOffset(enumConstantDirectory);
            enumConstantsOffset = Unsafe.objectFieldOffset(enumConstants);
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void reset(Class<?> cl) {
        Unsafe.putObject(cl, enumConstantDirectoryOffset, null);
        Unsafe.putObject(cl, enumConstantsOffset, null);
    }

}
