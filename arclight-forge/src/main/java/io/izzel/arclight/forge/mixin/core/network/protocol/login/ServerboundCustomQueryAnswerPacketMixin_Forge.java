package io.izzel.arclight.forge.mixin.core.network.protocol.login;

import io.izzel.arclight.common.mod.util.ArclightCustomQueryAnswerPayload;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.login.ServerboundCustomQueryAnswerPacket;
import net.minecraft.network.protocol.login.custom.CustomQueryAnswerPayload;
import net.minecraft.network.protocol.login.custom.DiscardedQueryAnswerPayload;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerboundCustomQueryAnswerPacket.class)
public class ServerboundCustomQueryAnswerPacketMixin_Forge {

    @Shadow @Final private static int MAX_PAYLOAD_SIZE;

    @Inject(method = "readPayload", cancellable = true, at = @At("HEAD"))
    private static void readResponse(int id, FriendlyByteBuf buf, CallbackInfoReturnable<CustomQueryAnswerPayload> cir) {
        boolean hasPayload = buf.readBoolean();
        if (!hasPayload) {
            cir.setReturnValue(DiscardedQueryAnswerPayload.INSTANCE);
            return;
        }
        int i = buf.readableBytes();
        if (i >= 0 && i < MAX_PAYLOAD_SIZE) {
            var payload = Unpooled.buffer(i);
            buf.readBytes(payload);
            cir.setReturnValue(new ArclightCustomQueryAnswerPayload(payload));
        } else {
            throw new IllegalArgumentException("Payload may not be larger than 1048576 bytes");
        }
    }
}
