package io.izzel.arclight.fabric.mod.plugin.messaging;

import io.izzel.arclight.common.bridge.core.server.level.ServerPlayerBridge;
import io.izzel.arclight.common.bridge.core.server.network.ServerCommonPacketListenerImplBridge;
import io.izzel.arclight.common.mod.plugin.messaging.ArclightPluginChannel;
import io.izzel.arclight.common.mod.plugin.messaging.ArclightRawPayload;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import org.bukkit.craftbukkit.v.entity.CraftPlayer;
import org.bukkit.plugin.Plugin;

public record ArclightFabricPayloadHandler(ArclightPluginChannel<ArclightFabricPayloadHandler> channel) implements FabricPayloadHandler {

    @Override
    public void updateChannel() {
    }

    @Override
    public void receive(ArclightRawPayload pkt, ServerPlayNetworking.Context ctx) {
        ctx.server().executeIfPossible(() -> {
            var bukkit = ((ServerPlayerBridge)ctx.player()).bridge$getBukkitEntity();
            channel.dispatchMessage(bukkit, pkt.arclight$leak());
        });
    }

    @Override
    public void receive(ArclightRawPayload pkt, ServerConfigurationNetworking.Context ctx) {
        ctx.server().executeIfPossible(() -> {
            var bukkit = ((ServerCommonPacketListenerImplBridge)ctx.networkHandler()).bridge$getCraftPlayer();
            channel.dispatchMessage(bukkit, pkt.arclight$leak());
        });
    }

    @Override
    public void sendCustomPayload(Plugin src, CraftPlayer dst, byte[] data) {
        var player = dst.getHandle();
        if (player.connection != null) {
            ServerPlayNetworking.send(player, new ArclightRawPayload(channel.getType(), data));
        }
    }
}
