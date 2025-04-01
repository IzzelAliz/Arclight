package io.izzel.arclight.forge.mod.plugin.messaging;

import io.izzel.arclight.common.bridge.core.entity.player.ServerPlayerEntityBridge;
import io.izzel.arclight.common.bridge.core.network.common.ServerCommonPacketListenerBridge;
import io.izzel.arclight.common.mod.plugin.messaging.ArclightPluginChannel;
import io.izzel.arclight.common.mod.plugin.messaging.ArclightRawPayload;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.network.NetworkProtocol;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;
import org.bukkit.craftbukkit.v.entity.CraftPlayer;
import org.bukkit.plugin.Plugin;

public class ArclightForgePayloadHandler implements ForgePayloadHandler {

    private final ArclightPluginChannel<?> bukkit;
    private SimpleChannel forge;

    public ArclightForgePayloadHandler(ArclightPluginChannel<?> bukkit) {
        this.bukkit = bukkit;
    }

    public void initialize(SimpleChannel unconfigured) {
        forge = unconfigured
                .messageBuilder(ArclightRawPayload.class, NetworkProtocol.PLAY)
                .codec(bukkit.getCast())
                .consumerMainThread(this)
                .add();
    }

    @Override
    public void accept(ArclightRawPayload payload, CustomPayloadEvent.Context ctx) {
        // Already on main thread thanks to SimpleChannel
        var listener = ctx.getConnection().getPacketListener();
        if (listener instanceof ServerCommonPacketListenerBridge bridge) {
            var craftbukkit = bridge.bridge$getCraftPlayer();
            bukkit.dispatchMessage(craftbukkit, payload.getData().array());
        }
    }

    @Override
    public void sendCustomPayload(Plugin src, CraftPlayer dst, byte[] data) {
        forge.send(new ArclightRawPayload(bukkit.getType(), data), PacketDistributor.PLAYER.with(dst.getHandle()));
    }

    @Override
    public ArclightPluginChannel<?> channel() {
        return bukkit;
    }
}
