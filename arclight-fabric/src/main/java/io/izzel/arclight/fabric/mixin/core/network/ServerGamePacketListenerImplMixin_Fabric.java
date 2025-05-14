package io.izzel.arclight.fabric.mixin.core.network;

import io.izzel.arclight.common.bridge.bukkit.MessengerBridge;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.bukkit.Bukkit;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public class ServerGamePacketListenerImplMixin_Fabric {

    @Inject(method = "handleCustomPayload", at = @At("TAIL"))
    private void arclight$recordUnknown(ServerboundCustomPayloadPacket packet, CallbackInfo ci) {
        var recorder = ((MessengerBridge) Bukkit.getMessenger()).arclight$getPacketRecorder();
        recorder.recordUnknown(packet.payload().type().id().toString());
        recorder.update();
    }
}
