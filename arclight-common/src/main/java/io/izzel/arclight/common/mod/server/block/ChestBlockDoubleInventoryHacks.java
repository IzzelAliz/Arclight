package io.izzel.arclight.common.mod.server.block;

import io.izzel.arclight.common.bridge.core.world.level.block.DoubleChestMenuProviderBridge;
import net.minecraft.world.CompoundContainer;

public class ChestBlockDoubleInventoryHacks {

    public static CompoundContainer get(Object obj) {
        return ((DoubleChestMenuProviderBridge) obj).bridge$getDoubleChestContainer();
    }

    public static boolean isInstance(Object obj) {
        return obj instanceof DoubleChestMenuProviderBridge;
    }
}
