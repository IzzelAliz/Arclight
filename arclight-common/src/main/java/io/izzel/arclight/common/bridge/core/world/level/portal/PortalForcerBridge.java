package io.izzel.arclight.common.bridge.core.world.level.portal;

import net.minecraft.world.entity.Entity;

public interface PortalForcerBridge {

    void bridge$pushSearchRadius(int searchRadius);

    void bridge$pushPortalCreate(Entity entity, int createRadius);
}
