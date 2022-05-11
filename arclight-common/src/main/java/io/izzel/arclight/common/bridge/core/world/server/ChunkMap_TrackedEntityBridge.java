package io.izzel.arclight.common.bridge.core.world.server;

import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.world.entity.Entity;

public interface ChunkMap_TrackedEntityBridge {

    ServerEntity bridge$getServerEntity();

    Entity bridge$getEntity();

    SectionPos bridge$getLastSectionPos();

    void bridge$setLastSectionPos(SectionPos pos);
}
