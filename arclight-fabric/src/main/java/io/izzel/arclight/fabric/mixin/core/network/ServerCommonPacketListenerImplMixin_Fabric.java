package io.izzel.arclight.fabric.mixin.core.network;

import io.izzel.arclight.common.bridge.core.network.common.ServerCommonPacketListenerBridge;
import net.fabricmc.fabric.impl.networking.payload.RetainedPayload;
import net.fabricmc.fabric.impl.networking.payload.UntypedPayload;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ServerCommonPacketListenerImpl.class)
public abstract class ServerCommonPacketListenerImplMixin_Fabric implements ServerCommonPacketListenerBridge {

    @Override
    public FriendlyByteBuf bridge$getDiscardedData(ServerboundCustomPayloadPacket packet) {
        if (packet.payload() instanceof RetainedPayload r) {
            return r.buf();
        } else if (packet.payload() instanceof UntypedPayload r) {
            return r.buffer();
        }
        return null;
    }
}
