package io.izzel.arclight.common.bridge.core.network.handshake;

import io.izzel.arclight.common.bridge.core.network.common.ServerCommonPacketListenerBridge;
import net.minecraft.network.protocol.handshake.ClientIntentionPacket;

public interface ServerHandshakeNetHandlerBridge extends ServerCommonPacketListenerBridge {
    default boolean bridge$forge$handleSpecialLogin(ClientIntentionPacket packet) {
        return true;
    }
}
