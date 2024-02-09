package io.izzel.arclight.neoforge.mixin.core.network;

import io.izzel.arclight.common.bridge.core.network.ServerStatusPacketListenerBridge;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.server.network.ServerStatusPacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;

import java.util.Optional;

@Mixin(ServerStatusPacketListenerImpl.class)
public abstract class ServerStatusNetHandlerMixin_NeoForge implements ServerStatusPacketListenerBridge {
    @Override
    public ServerStatus bridge$platform$createServerStatus(Component description, Optional<ServerStatus.Players> players, Optional<ServerStatus.Version> version, Optional<ServerStatus.Favicon> favicon, boolean enforcesSecureChat) {
        return new ServerStatus(description, players, version, favicon, enforcesSecureChat, true);
    }
}
