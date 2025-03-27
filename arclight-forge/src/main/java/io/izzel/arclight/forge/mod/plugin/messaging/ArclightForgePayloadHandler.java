package io.izzel.arclight.forge.mod.plugin.messaging;

import io.izzel.arclight.common.bridge.core.entity.player.ServerPlayerEntityBridge;
import io.izzel.arclight.common.mod.plugin.messaging.ArclightPluginChannel;
import io.izzel.arclight.common.mod.plugin.messaging.ArclightRawPayload;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.network.NetworkProtocol;
import net.minecraftforge.network.SimpleChannel;

public class ArclightForgePayloadHandler implements ForgePayloadHandler {

    private final ArclightPluginChannel bukkit;
    private SimpleChannel forge;

    public ArclightForgePayloadHandler(ArclightPluginChannel bukkit) {
        this.bukkit = bukkit;
    }

    public void initialize(SimpleChannel unconfigured) {
        forge = unconfigured
                .messageBuilder(ArclightRawPayload.class, NetworkProtocol.PLAY)
                .codec(bukkit.getCast())
                .consumerMainThread(this)
                .add();
    }

    public SimpleChannel getSimpleChannel() {
        return forge;
    }

    public ArclightPluginChannel getPluginChannel() {
        return bukkit;
    }

    @Override
    public void accept(ArclightRawPayload payload, CustomPayloadEvent.Context ctx) {
        // Already on main thread thanks to SimpleChannel
        var craftbukkit = ((ServerPlayerEntityBridge)ctx.getSender()).bridge$getBukkitEntity();
        bukkit.dispatchMessage(craftbukkit, payload.raw());
    }

}
