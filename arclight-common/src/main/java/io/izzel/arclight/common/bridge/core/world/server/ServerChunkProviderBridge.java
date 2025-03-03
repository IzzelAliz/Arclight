package io.izzel.arclight.common.bridge.core.world.server;

import java.io.IOException;
import net.minecraft.server.level.ThreadedLevelLightEngine;

public interface ServerChunkProviderBridge {

    void bridge$close(boolean save) throws IOException;

    void bridge$purgeUnload();

    boolean bridge$tickDistanceManager();

    boolean bridge$isChunkLoaded(int x, int z);

    ThreadedLevelLightEngine bridge$getLightManager();

    void bridge$setViewDistance(int viewDistance);

    void bridge$setSimulationDistance(int simDistance);
}
