package io.izzel.arclight.common.bridge.core.world.inventory;

import io.izzel.arclight.common.bridge.core.world.inventory.ContainerLevelAccessBridge;
import net.minecraft.world.inventory.ContainerLevelAccess;
import org.bukkit.Location;

public interface PosContainerBridge extends AbstractContainerMenuBridge {

    ContainerLevelAccess bridge$getWorldPos();

    default Location bridge$getWorldLocation() {
        return ((ContainerLevelAccessBridge) bridge$getWorldPos()).bridge$getLocation();
    }
}
