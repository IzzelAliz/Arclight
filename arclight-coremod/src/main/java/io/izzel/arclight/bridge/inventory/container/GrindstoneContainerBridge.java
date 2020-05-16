package io.izzel.arclight.bridge.inventory.container;

import net.minecraft.util.IWorldPosCallable;

public interface GrindstoneContainerBridge extends ContainerBridge {

    IWorldPosCallable bridge$getContainerAccess();
}
