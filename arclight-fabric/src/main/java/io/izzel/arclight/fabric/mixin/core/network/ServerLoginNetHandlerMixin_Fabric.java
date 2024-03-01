package io.izzel.arclight.fabric.mixin.core.network;

import io.izzel.arclight.common.bridge.core.network.login.ServerLoginNetHandlerBridge;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.impl.networking.NetworkHandlerExtensions;
import net.fabricmc.fabric.impl.networking.payload.PacketByteBufLoginQueryResponse;
import net.fabricmc.fabric.impl.networking.server.ServerLoginNetworkAddon;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.login.ServerboundCustomQueryAnswerPacket;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ServerLoginPacketListenerImpl.class)
public abstract class ServerLoginNetHandlerMixin_Fabric implements ServerLoginNetHandlerBridge, NetworkHandlerExtensions {

    @Override
    public FriendlyByteBuf bridge$getDiscardedQueryAnswerData(ServerboundCustomQueryAnswerPacket payload) {
        if (payload.payload() instanceof PacketByteBufLoginQueryResponse query) {
            return new FriendlyByteBuf(Unpooled.wrappedBuffer(Unpooled.copyBoolean(true), query.data().slice()));
        }
        return null;
    }

    @Override
    public void bridge$platform$onCustomQuery(ServerboundCustomQueryAnswerPacket payload) {
        ((ServerLoginNetworkAddon) this.getAddon()).handle(payload);
    }
}
