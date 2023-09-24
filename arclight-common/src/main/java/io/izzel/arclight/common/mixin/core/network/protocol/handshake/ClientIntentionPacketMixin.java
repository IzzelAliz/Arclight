package io.izzel.arclight.common.mixin.core.network.protocol.handshake;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.handshake.ClientIntentionPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ClientIntentionPacket.class)
public class ClientIntentionPacketMixin {

    @Redirect(method = "<init>(Lnet/minecraft/network/FriendlyByteBuf;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/FriendlyByteBuf;readUtf(I)Ljava/lang/String;"))
    private static String arclight$bungeeHostname(FriendlyByteBuf packetBuffer, int maxLength) {
        return packetBuffer.readUtf(Short.MAX_VALUE);
    }
}
