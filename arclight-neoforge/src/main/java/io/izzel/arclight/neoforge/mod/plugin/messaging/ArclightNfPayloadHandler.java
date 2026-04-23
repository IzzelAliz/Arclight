package io.izzel.arclight.neoforge.mod.plugin.messaging;

import io.izzel.arclight.common.bridge.core.server.level.ServerPlayerBridge;
import io.izzel.arclight.common.mod.plugin.messaging.ArclightPluginChannel;
import io.izzel.arclight.common.mod.plugin.messaging.ArclightRawPayload;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.bukkit.craftbukkit.v.entity.CraftPlayer;
import org.bukkit.plugin.Plugin;

public record ArclightNfPayloadHandler(ArclightPluginChannel<ArclightNfPayloadHandler> channel) implements NeoforgePayloadHandler {
    @Override
    public void handle(ArclightRawPayload pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            var bukkit = ((ServerPlayerBridge)ctx.player()).bridge$getBukkitEntity();
            channel.dispatchMessage(bukkit, pkt.arclight$leak());
        });
    }

    @Override
    public void updateChannel() {
        ArclightNfMessaging.updateChannel(channel);
    }

    @Override
    public void sendCustomPayload(Plugin src, CraftPlayer dst, byte[] data) {
        PacketDistributor.sendToPlayer(dst.getHandle(), new ArclightRawPayload(channel.getType(), data));
    }
}
