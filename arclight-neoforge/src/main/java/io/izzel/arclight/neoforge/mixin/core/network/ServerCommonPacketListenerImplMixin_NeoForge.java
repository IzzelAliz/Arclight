package io.izzel.arclight.neoforge.mixin.core.network;

import io.izzel.arclight.common.bridge.core.network.common.ServerCommonPacketListenerBridge;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.DiscardedPayload;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ServerCommonPacketListenerImpl.class)
public abstract class ServerCommonPacketListenerImplMixin_NeoForge implements ServerCommonPacketListenerBridge {

    @Override
    public FriendlyByteBuf bridge$getDiscardedData(DiscardedPayload payload) {
        // Todo: Payload data.
        return null;
    }
}
