package io.izzel.arclight.common.bridge.inventory.container;

import io.izzel.arclight.common.bridge.util.IWorldPosCallableBridge;
import net.minecraft.util.IWorldPosCallable;
import org.bukkit.Location;

public interface PosContainerBridge extends ContainerBridge {

    IWorldPosCallable bridge$getWorldPos();

    default Location bridge$getWorldLocation() {
        return ((IWorldPosCallableBridge) bridge$getWorldPos()).bridge$getLocation();
    }
}
