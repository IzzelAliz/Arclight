package io.izzel.arclight.common.bridge.core.inventory.container;

import io.izzel.arclight.common.bridge.core.util.IWorldPosCallableBridge;
import net.minecraft.world.inventory.ContainerLevelAccess;
import org.bukkit.Location;

public interface PosContainerBridge extends ContainerBridge {

    ContainerLevelAccess bridge$getWorldPos();

    default Location bridge$getWorldLocation() {
        return ((IWorldPosCallableBridge) bridge$getWorldPos()).bridge$getLocation();
    }
}
