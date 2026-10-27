package io.izzel.arclight.common.bridge.core.world.server;

import java.io.IOException;
import net.minecraft.server.level.ThreadedLevelLightEngine;

public interface ServerChunkProviderBridge {

    void bridge$close(boolean save) throws IOException;

    void bridge$purgeUnload();
    boolean bridge$tickDistanceManager();

    boolean bridge$isChunkLoaded(int x, int z);

    ThreadedLevelLightEngine bridge$getLightManager();

    boolean bridge$getSpawnFriendlies();

    void bridge$setSpawnFriendlies(boolean spawnFriendlies);

    void bridge$setViewDistance(int viewDistance);

    void bridge$setSimulationDistance(int simDistance);

    default boolean isChunkLoaded(int x, int z) {
        return this.bridge$isChunkLoaded(x, z);
    }

    default void purgeUnload() {
        this.bridge$purgeUnload();
    }

}