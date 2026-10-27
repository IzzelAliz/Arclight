package io.izzel.arclight.common.mixin.core.server.network;

import com.destroystokyo.paper.network.StandardPaperServerListPingEventImpl;
import io.izzel.arclight.common.bridge.core.server.network.ServerStatusPacketListenerImplBridge;
import io.izzel.arclight.common.mod.server.ArclightServer;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.network.ServerStatusPacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ServerStatusPacketListenerImpl.class)
public class ServerStatusPacketListenerImplMixin implements ServerStatusPacketListenerImplBridge {

    @Redirect(method = "handleStatusRequest", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/Connection;send(Lnet/minecraft/network/protocol/Packet;)V"))
    private void arclight$handleServerPing(Connection networkManager, Packet<?> packetIn) {
        StandardPaperServerListPingEventImpl.processRequest(ArclightServer.getMinecraftServer(), networkManager, this);
    }
}
