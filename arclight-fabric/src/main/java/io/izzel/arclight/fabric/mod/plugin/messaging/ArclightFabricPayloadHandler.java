package io.izzel.arclight.fabric.mod.plugin.messaging;

import io.izzel.arclight.common.bridge.core.entity.player.ServerPlayerEntityBridge;
import io.izzel.arclight.common.bridge.core.network.common.ServerCommonPacketListenerBridge;
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
            var bukkit = ((ServerPlayerEntityBridge)ctx.player()).bridge$getBukkitEntity();
            channel.dispatchMessage(bukkit, pkt.data().array());
        });
    }

    @Override
    public void receive(ArclightRawPayload pkt, ServerConfigurationNetworking.Context ctx) {
        ctx.server().executeIfPossible(() -> {
            var bukkit = ((ServerCommonPacketListenerBridge)ctx.networkHandler()).bridge$getCraftPlayer();
            channel.dispatchMessage(bukkit, pkt.data().array());
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
