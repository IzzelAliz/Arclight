package io.izzel.arclight.fabric.mixin.core.server.network;

import io.izzel.arclight.common.bridge.core.server.network.ServerCommonPacketListenerImplBridge;
import io.izzel.arclight.common.mod.plugin.messaging.PacketRecorder;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerCommonPacketListenerImpl.class)
public abstract class ServerCommonPacketListenerImplMixin_Fabric implements ServerCommonPacketListenerImplBridge {

    @Inject(method = "handleCustomPayload", at = @At("TAIL"))
    private void arclight$handleUnknownPayload(ServerboundCustomPayloadPacket packet, CallbackInfo ci) {
        PacketRecorder.recordUnknown(packet.payload().type().id());
    }
}
