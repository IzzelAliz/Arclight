package io.izzel.arclight.fabric.mixin.core.network;

import io.izzel.arclight.common.bridge.core.network.login.ServerLoginNetHandlerBridge;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.login.custom.DiscardedQueryAnswerPayload;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ServerLoginPacketListenerImpl.class)
public abstract class ServerLoginNetHandlerMixin_Fabric implements ServerLoginNetHandlerBridge {

    @Override
    public FriendlyByteBuf bridge$getDiscardedQueryAnswerData(DiscardedQueryAnswerPayload payload) {
        // Todo: Payload data.
        return null;
    }
}
