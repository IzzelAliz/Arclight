package io.izzel.arclight.fabric.mixin.core.server.network;

import io.izzel.arclight.common.bridge.bukkit.MessengerBridge;
import io.izzel.arclight.common.bridge.core.server.network.ServerCommonPacketListenerImplBridge;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import org.bukkit.Bukkit;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerCommonPacketListenerImpl.class)
public abstract class ServerCommonPacketListenerImplMixin_Fabric implements ServerCommonPacketListenerImplBridge {

    @Inject(method = "handleCustomPayload", at = @At("TAIL"))
    private void arclight$handleUnknownPayload(ServerboundCustomPayloadPacket packet, CallbackInfo ci) {
        var recorder = ((MessengerBridge) Bukkit.getMessenger()).arclight$getPacketRecorder();
        recorder.recordUnknown(packet.payload().type().id());
        recorder.update();
    }
}
