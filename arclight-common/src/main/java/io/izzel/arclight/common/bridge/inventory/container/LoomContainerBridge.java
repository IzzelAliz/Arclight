package io.izzel.arclight.common.bridge.inventory.container;

import net.minecraft.util.IWorldPosCallable;

public interface LoomContainerBridge extends ContainerBridge {

    IWorldPosCallable bridge$getWorldPos();
}
