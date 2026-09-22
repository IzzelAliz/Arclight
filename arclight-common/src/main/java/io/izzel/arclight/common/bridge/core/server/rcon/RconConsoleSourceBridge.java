package io.izzel.arclight.common.bridge.core.server.rcon;

public interface RconConsoleSourceBridge {
    void bridge$setSocketAddress(java.net.SocketAddress address);


    void bridge$sendMessage(String message);
}
