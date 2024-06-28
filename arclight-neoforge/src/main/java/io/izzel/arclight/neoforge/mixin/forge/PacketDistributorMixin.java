package io.izzel.arclight.neoforge.mixin.forge;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PacketDistributor.class)
public abstract class PacketDistributorMixin {

    @Inject(method = "sendToPlayer", cancellable = true, at = @At("HEAD"))
    private static void arclight$returnIfNotConnected(ServerPlayer player, CustomPacketPayload payload, CustomPacketPayload[] payloads, CallbackInfo ci) {
        if (player.connection == null) {
            ci.cancel();
        }
    }
}
