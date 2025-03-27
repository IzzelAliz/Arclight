package io.izzel.arclight.fabric.mod.plugin.messaging;

import io.izzel.arclight.common.bridge.core.entity.player.ServerPlayerEntityBridge;
import io.izzel.arclight.common.mod.plugin.messaging.ArclightPluginChannel;
import io.izzel.arclight.common.mod.plugin.messaging.ArclightRawPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public record ArclightFabricPayloadHandler(ArclightPluginChannel registry) implements ServerPlayNetworking.PlayPayloadHandler<ArclightRawPayload> {
    @Override
    public void receive(ArclightRawPayload pkt, ServerPlayNetworking.Context ctx) {
        ctx.server().executeIfPossible(() -> {
            var bukkit = ((ServerPlayerEntityBridge)ctx.player()).bridge$getBukkitEntity();
            registry.dispatchMessage(bukkit, pkt.raw());
        });
    }
}
