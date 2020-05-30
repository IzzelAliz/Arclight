package io.izzel.arclight.common.bridge.inventory.container;

import net.minecraft.util.IWorldPosCallable;

public interface EnchantmentContainerBridge extends ContainerBridge {

    IWorldPosCallable bridge$getContainerAccess();
}
