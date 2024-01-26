package io.izzel.arclight.common.bridge.core.network;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.status.ServerStatus;

import java.util.Optional;

public interface ServerStatusPacketListenerBridge {
    default ServerStatus bridge$platform$createServerStatus(Component description, Optional<ServerStatus.Players> players, Optional<ServerStatus.Version> version, Optional<ServerStatus.Favicon> favicon, boolean enforcesSecureChat) {
        return new ServerStatus(description, players, version, favicon, enforcesSecureChat);
    }
}
