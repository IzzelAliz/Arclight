package io.izzel.arclight.common.bridge.core.server.network;

import io.papermc.paper.connection.PlayerConfigurationConnection;

public interface ServerConfigurationPacketListenerImplBridge {

    PlayerConfigurationConnection bridge$getConfigurationConnection();

}
