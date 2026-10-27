package io.izzel.arclight.forge.mixin.core.server.network;

import io.izzel.arclight.common.bridge.core.server.network.ServerStatusPacketListenerImplBridge;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.server.network.ServerStatusPacketListenerImpl;
import net.minecraftforge.network.ServerStatusPing;
import org.spongepowered.asm.mixin.Mixin;

import java.util.Optional;

@Mixin(ServerStatusPacketListenerImpl.class)
public abstract class ServerStatusPacketListenerImplMixin_Forge implements ServerStatusPacketListenerImplBridge {
    @Override
    public ServerStatus bridge$platform$createServerStatus(Component description, Optional<ServerStatus.Players> players, Optional<ServerStatus.Version> version, Optional<ServerStatus.Favicon> favicon, boolean enforcesSecureChat) {
        return new ServerStatus(description, players, version, favicon, enforcesSecureChat, Optional.of(new ServerStatusPing()));
    }
}
