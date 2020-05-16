package io.izzel.arclight.bridge.inventory.container;

import net.minecraft.util.IWorldPosCallable;

public interface LoomContainerBridge extends ContainerBridge {

    IWorldPosCallable bridge$getWorldPos();
}
