package io.izzel.arclight.forge.mixin.core.network;

import io.izzel.arclight.common.bridge.core.network.common.ServerCommonPacketListenerBridge;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.DiscardedPayload;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ServerCommonPacketListenerImpl.class)
public abstract class ServerCommonPacketListenerImplMixin_Forge implements ServerCommonPacketListenerBridge {

    @Override
    public FriendlyByteBuf bridge$getDiscardedData(ServerboundCustomPayloadPacket packet) {
        return packet.getInternalData();
    }
}
