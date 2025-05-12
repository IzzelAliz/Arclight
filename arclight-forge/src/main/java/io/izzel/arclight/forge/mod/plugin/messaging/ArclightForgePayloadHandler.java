package io.izzel.arclight.forge.mod.plugin.messaging;

import com.google.common.base.Preconditions;
import io.izzel.arclight.common.bridge.core.network.common.ServerCommonPacketListenerBridge;
import io.izzel.arclight.common.mod.ArclightConstants;
import io.izzel.arclight.common.mod.plugin.messaging.ArclightPluginChannel;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.network.EventNetworkChannel;
import net.minecraftforge.network.PacketDistributor;
import org.bukkit.craftbukkit.v.entity.CraftPlayer;
import org.bukkit.plugin.Plugin;

public class ArclightForgePayloadHandler implements ForgePayloadHandler {

    private final ArclightPluginChannel<?> bukkit;
    private EventNetworkChannel forge;

    public ArclightForgePayloadHandler(ArclightPluginChannel<?> bukkit) {
        this.bukkit = bukkit;
    }

    public void initialize(EventNetworkChannel unconfigured) {
        forge = unconfigured.addListener(this);
    }

    @Override
    public void sendCustomPayload(Plugin src, CraftPlayer dst, byte[] data) {
        forge.send(new FriendlyByteBuf(Unpooled.copiedBuffer(data)), PacketDistributor.PLAYER.with(dst.getHandle()));
    }

    @Override
    public ArclightPluginChannel<?> channel() {
        return bukkit;
    }

    @Override
    public void updateChannel() {

    }

    @Override
    public void accept(CustomPayloadEvent event) {
        var ctx = event.getSource();
        ctx.setPacketHandled(true);
        var buf = event.getPayload();
        final var max = ArclightConstants.MAX_C2S_CUSTOM_PAYLOAD_SIZE;
        Preconditions.checkArgument(buf.readableBytes() <= max, "Custom payload size may not be larger than " + max);

        byte[] data = new byte[buf.readableBytes()];
        buf.readBytes(data);

        ctx.enqueueWork(() -> {
            var listener = ctx.getConnection().getPacketListener();
            if (listener instanceof ServerCommonPacketListenerBridge bridge) {
                var craftbukkit = bridge.bridge$getCraftPlayer();
                bukkit.dispatchMessage(craftbukkit, data);
            }
        });
    }
}
