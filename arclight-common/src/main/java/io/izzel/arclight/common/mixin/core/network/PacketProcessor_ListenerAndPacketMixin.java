package io.izzel.arclight.common.mixin.core.network;

import io.izzel.arclight.common.bridge.core.server.network.ServerCommonPacketListenerImplBridge;
import net.minecraft.network.PacketListener;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.network.PacketProcessor$ListenerAndPacket")
public abstract class PacketProcessor_ListenerAndPacketMixin {

    @Shadow @Final private PacketListener listener;

    @Inject(method = "handle", cancellable = true, at = @At("HEAD"))
    private void arclight$skipPacketAfterDisconnect(CallbackInfo ci) {
        if (this.listener instanceof ServerCommonPacketListenerImpl && ((ServerCommonPacketListenerImplBridge) this.listener).bridge$processedDisconnect()) {
            ci.cancel();
        }
    }
}