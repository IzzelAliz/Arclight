package io.izzel.arclight.fabric.mixin.core.server.network;

import io.izzel.arclight.common.mod.plugin.messaging.PacketRecorder;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public class ServerGamePacketListenerImplMixin_Fabric {

    @Inject(method = "handleCustomPayload", at = @At("TAIL"))
    private void arclight$recordUnknown(ServerboundCustomPayloadPacket packet, CallbackInfo ci) {
        PacketRecorder.recordUnknown(packet.payload().type().id());
    }
}
