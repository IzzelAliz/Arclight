package io.izzel.arclight.common.mod.server.block;

import net.minecraft.world.level.block.DispenserBlock;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

public class DispenserBlockHooks {

    private static final VarHandle H_EVENT_FIRED;

    static {
        try {
            var field = DispenserBlock.class.getDeclaredField("eventFired");
            H_EVENT_FIRED = MethodHandles.lookup().unreflectVarHandle(field);
        } catch (Throwable t) {
            throw new ExceptionInInitializerError(t);
        }
    }

    public static boolean isEventFired() {
        return (boolean) H_EVENT_FIRED.get();
    }

    public static void setEventFired(boolean b) {
        H_EVENT_FIRED.set(b);
    }
}
