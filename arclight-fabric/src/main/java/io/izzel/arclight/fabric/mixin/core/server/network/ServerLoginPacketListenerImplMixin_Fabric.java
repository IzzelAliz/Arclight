package io.izzel.arclight.fabric.mixin.core.server.network;

import io.izzel.arclight.common.bridge.core.server.network.ServerLoginPacketListenerImplBridge;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.impl.networking.NetworkHandlerExtensions;
import net.fabricmc.fabric.impl.networking.payload.PacketByteBufLoginQueryResponse;
import net.fabricmc.fabric.impl.networking.server.ServerLoginNetworkAddon;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.login.ServerboundCustomQueryAnswerPacket;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ServerLoginPacketListenerImpl.class)
public abstract class ServerLoginPacketListenerImplMixin_Fabric implements ServerLoginPacketListenerImplBridge, NetworkHandlerExtensions {

    @Override
    public FriendlyByteBuf arclight$platform$customQAData(ServerboundCustomQueryAnswerPacket packet) {
        if (packet.payload() instanceof PacketByteBufLoginQueryResponse query) {
            // Data is consumed before we handle it
            return new FriendlyByteBuf(Unpooled.wrappedBuffer(Unpooled.copyBoolean(true), query.data().readerIndex(0)));
        }
        return null;
    }

    @Override
    public void arclight$platform$onCustomQA(ServerboundCustomQueryAnswerPacket payload) {
        ((ServerLoginNetworkAddon) this.getAddon()).handle(payload);
    }
}
