package io.izzel.arclight.forge.mixin.core.network.protocol.login;

import io.izzel.arclight.common.mod.util.ArclightCustomQueryAnswerPayload;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.login.ServerboundCustomQueryAnswerPacket;
import net.minecraft.network.protocol.login.custom.CustomQueryAnswerPayload;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerboundCustomQueryAnswerPacket.class)
public class ServerboundCustomQueryAnswerPacketMixin_Forge {

    @Shadow @Final private static int MAX_PAYLOAD_SIZE;

    // For FFAPI; See ServerLoginPacketListenerImplMixin
    @Inject(method = "readUnknownPayload", cancellable = true, at = @At("HEAD"))
    private static void readResponse(FriendlyByteBuf buf, CallbackInfoReturnable<CustomQueryAnswerPayload> cir) {
        cir.setReturnValue(ArclightCustomQueryAnswerPayload.tryRead(buf, MAX_PAYLOAD_SIZE));
    }
}
