package io.izzel.arclight.common.bridge.core.world.chunk;

import org.bukkit.persistence.PersistentDataContainer;

public interface ChunkAccessBridge {

    long bridge$getCoordinateKey();

    PersistentDataContainer bridge$getPersistentDataContainer();
}