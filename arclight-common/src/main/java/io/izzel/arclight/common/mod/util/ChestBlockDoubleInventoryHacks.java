package io.izzel.arclight.common.mod.util;

import io.izzel.arclight.api.Unsafe;
import net.minecraft.inventory.DoubleSidedInventory;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.tileentity.ChestTileEntity;

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
            ctor = Unsafe.lookup().findConstructor(cl, MethodType.methodType(void.class, ChestTileEntity.class, ChestTileEntity.class, DoubleSidedInventory.class));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static DoubleSidedInventory get(Object obj) {
        return (DoubleSidedInventory) Unsafe.getObject(obj, offset);
    }

    public static boolean isInstance(Object obj) {
        return cl.isInstance(obj);
    }

    public static INamedContainerProvider create(ChestTileEntity entity, ChestTileEntity entity1, DoubleSidedInventory inventory) {
        try {
            return (INamedContainerProvider) ctor.invoke(entity, entity1, inventory);
        } catch (Throwable throwable) {
            throw new RuntimeException(throwable);
        }
    }
}
