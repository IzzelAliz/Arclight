package io.izzel.arclight.bridge.world.server;

import net.minecraft.world.server.ServerWorldLightManager;

import java.io.IOException;

public interface ServerChunkProviderBridge {

    void bridge$close(boolean save) throws IOException;

    void bridge$purgeUnload();

    boolean bridge$tickDistanceManager();

    ServerWorldLightManager bridge$getLightManager();
}
