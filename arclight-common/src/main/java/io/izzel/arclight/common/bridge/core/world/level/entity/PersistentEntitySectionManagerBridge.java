package io.izzel.arclight.common.bridge.core.world.level.entity;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;

import java.io.IOException;
import java.util.List;

public interface PersistentEntitySectionManagerBridge {

    List<Entity> getEntities(ChunkPos pos);

    /** Snapshots all entities in existing sections, including hidden sections. */
    List<Entity> bridge$getExistingEntities();

    boolean isPending(long chunkPos);

    void close(boolean save) throws IOException;
}
