package io.izzel.arclight.common.bridge.core.world;

import net.minecraft.world.entity.Entity;

public interface TeleporterBridge {

    void bridge$pushSearchRadius(int searchRadius);

    void bridge$pushPortalCreate(Entity entity, int createRadius);
}
