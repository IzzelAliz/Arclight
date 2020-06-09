package io.izzel.arclight.common.mixin.core.network.handshake.client;

import net.minecraft.network.PacketBuffer;
import net.minecraft.network.handshake.client.CHandshakePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(CHandshakePacket.class)
public class CHandshakePacketMixin {

    @Redirect(method = "readPacketData", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/PacketBuffer;readString(I)Ljava/lang/String;"))
    private String arclight$bungeeHostname(PacketBuffer packetBuffer, int maxLength) {
        return packetBuffer.readString(Short.MAX_VALUE);
    }
}
