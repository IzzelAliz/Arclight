package io.izzel.arclight.common.mixin.core.network.protocol.common;

import io.izzel.arclight.common.bridge.core.network.ConnectionBridge;
import io.izzel.arclight.common.bridge.core.server.network.ServerCommonPacketListenerImplBridge;
import net.minecraft.network.protocol.common.ServerCommonPacketListener;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.BrandPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerboundCustomPayloadPacket.class)
public abstract class ServerboundCustomPayloadPacketMixin {

    @Shadow public abstract CustomPacketPayload payload();

    @Inject(method = "handle(Lnet/minecraft/network/protocol/common/ServerCommonPacketListener;)V", at = @At("HEAD"))
    private void arclight$observeClientBrand(ServerCommonPacketListener listener, CallbackInfo ci) {
        if (this.payload() instanceof BrandPayload brand && listener instanceof ServerCommonPacketListenerImplBridge bridge) {
            ((ConnectionBridge) bridge.bridge$getConnection()).bridge$setClientBrandName(brand.brand());
        }
    }
}