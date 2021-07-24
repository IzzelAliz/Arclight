package io.izzel.arclight.common.mod.server.block;

import io.izzel.arclight.api.Unsafe;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;

public class ChestBlockDoubleInventoryHacks {

    private static MethodHandle ctor;
    private static Class<?> cl;
    private static long offset;

    static {
        try {
            cl = Class.forName("net.minecraft.block.ChestBlock$DoubleInventory");
            Field field = cl.getDeclaredField("inventorylargechest");
            offset = Unsafe.objectFieldOffset(field);
            ctor = Unsafe.lookup().findConstructor(cl, MethodType.methodType(void.class, ChestBlockEntity.class, ChestBlockEntity.class, CompoundContainer.class));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static CompoundContainer get(Object obj) {
        return (CompoundContainer) Unsafe.getObject(obj, offset);
    }

    public static boolean isInstance(Object obj) {
        return cl.isInstance(obj);
    }

    public static MenuProvider create(ChestBlockEntity entity, ChestBlockEntity entity1, CompoundContainer inventory) {
        try {
            return (MenuProvider) ctor.invoke(entity, entity1, inventory);
        } catch (Throwable throwable) {
            throw new RuntimeException(throwable);
        }
    }
}
