package io.izzel.arclight.common.mod.server.block;

import io.izzel.arclight.api.Unsafe;
import net.minecraft.world.CompoundContainer;

import java.lang.reflect.Field;

public class ChestBlockDoubleInventoryHacks {

    private static final Class<?> cl;
    private static final long offset;

    static {
        try {
            cl = Class.forName("net/minecraft/world/level/block/ChestBlock$2$1");
            Field field = cl.getDeclaredField("inventorylargechest");
            offset = Unsafe.objectFieldOffset(field);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static CompoundContainer get(Object obj) {
        return (CompoundContainer) Unsafe.getObject(obj, offset);
    }

    public static boolean isInstance(Object obj) {
        return cl.isInstance(obj);
    }
}
