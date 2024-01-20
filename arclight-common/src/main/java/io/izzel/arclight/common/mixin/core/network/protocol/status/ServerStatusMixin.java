package io.izzel.arclight.common.mixin.core.network.protocol.status;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.status.ServerStatus;
import org.spongepowered.asm.mixin.Mixin;

import java.util.Optional;

@Mixin(ServerStatus.class)
public class ServerStatusMixin {

    public void arclight$constructor(Component description, Optional<ServerStatus.Players> players, Optional<ServerStatus.Version> version, Optional<ServerStatus.Favicon> favicon, boolean enforcesSecureChat, Optional<net.minecraftforge.network.ServerStatusPing> forgeData) {
        throw new RuntimeException();
    }

    public void arclight$constructor(Component description, Optional<ServerStatus.Players> players, Optional<ServerStatus.Version> version, Optional<ServerStatus.Favicon> favicon, boolean enforcesSecureChat) {
        arclight$constructor(description, players, version, favicon, enforcesSecureChat, Optional.empty());
    }
}
