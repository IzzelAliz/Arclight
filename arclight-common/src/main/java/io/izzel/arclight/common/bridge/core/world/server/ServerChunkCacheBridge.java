package io.izzel.arclight.common.bridge.core.world.server;

import net.minecraft.server.level.ThreadedLevelLightEngine;

public interface ServerChunkCacheBridge {

    boolean bridge$tickDistanceManager();

    ThreadedLevelLightEngine bridge$getLightManager();

    void bridge$setViewDistance(int viewDistance);

    void bridge$setSimulationDistance(int simDistance);
}