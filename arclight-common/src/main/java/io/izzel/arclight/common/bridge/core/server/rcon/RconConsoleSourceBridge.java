package io.izzel.arclight.common.bridge.core.server.rcon;

public interface RconConsoleSourceBridge {
    void bridge$setSocketAddress(java.net.SocketAddress address);

    java.net.SocketAddress bridge$getSocketAddress();


    void bridge$sendMessage(String message);
}
