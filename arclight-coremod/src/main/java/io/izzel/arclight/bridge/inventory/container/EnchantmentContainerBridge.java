package io.izzel.arclight.bridge.inventory.container;

import net.minecraft.util.IWorldPosCallable;

public interface EnchantmentContainerBridge extends ContainerBridge {

    IWorldPosCallable bridge$getContainerAccess();
}
