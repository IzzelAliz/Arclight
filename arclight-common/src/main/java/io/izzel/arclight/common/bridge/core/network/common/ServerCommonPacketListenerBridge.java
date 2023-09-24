package io.izzel.arclight.common.bridge.core.network.common;

import net.minecraft.server.level.ServerPlayer;

public interface ServerCommonPacketListenerBridge {

    boolean bridge$processedDisconnect();

    boolean bridge$isDisconnected();

    void bridge$setPlayer(ServerPlayer player);
}
