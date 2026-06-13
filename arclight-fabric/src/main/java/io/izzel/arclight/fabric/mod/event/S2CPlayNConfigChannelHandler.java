package io.izzel.arclight.fabric.mod.event;

import io.izzel.arclight.common.bridge.core.server.network.ServerCommonPacketListenerImplBridge;
import net.fabricmc.fabric.api.networking.v1.ClientboundConfigurationChannelEvents;
import net.fabricmc.fabric.api.networking.v1.ClientboundPlayChannelEvents;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

import java.util.List;

public class S2CPlayNConfigChannelHandler implements ClientboundPlayChannelEvents.Register, ClientboundPlayChannelEvents.Unregister, ClientboundConfigurationChannelEvents.Register, ClientboundConfigurationChannelEvents.Unregister {

    public static void register() {
        var handler = new S2CPlayNConfigChannelHandler();
        ClientboundPlayChannelEvents.REGISTER.register(handler);
        ClientboundPlayChannelEvents.UNREGISTER.register(handler);
        ClientboundConfigurationChannelEvents.REGISTER.register(handler);
        ClientboundConfigurationChannelEvents.UNREGISTER.register(handler);
    }

    private void register(MinecraftServer server, ServerCommonPacketListenerImpl listener, List<Identifier> channels) {
        server.executeIfPossible(() -> {
            var craftbukkit = ((ServerCommonPacketListenerImplBridge) listener).bridge$getCraftPlayer();
            for (var location : channels) {
                craftbukkit.addChannel(location.toString());
            }
        });
    }

    private void unregister(MinecraftServer server, ServerCommonPacketListenerImpl listener, List<Identifier> channels) {
        server.executeIfPossible(() -> {
            var craftbukkit = ((ServerCommonPacketListenerImplBridge) listener).bridge$getCraftPlayer();
            for (var location : channels) {
                craftbukkit.removeChannel(location.toString());
            }
        });
    }

    @Override
    public void onChannelRegister(ServerGamePacketListenerImpl handler, PacketSender sender, MinecraftServer server, List<Identifier> channels) {
        register(server, handler, channels);
    }

    @Override
    public void onChannelUnregister(ServerGamePacketListenerImpl handler, PacketSender sender, MinecraftServer server, List<Identifier> channels) {
        unregister(server, handler, channels);
    }

    @Override
    public void onChannelRegister(ServerConfigurationPacketListenerImpl handler, PacketSender sender, MinecraftServer server, List<Identifier> channels) {
        register(server, handler, channels);
    }

    @Override
    public void onChannelUnregister(ServerConfigurationPacketListenerImpl handler, PacketSender sender, MinecraftServer server, List<Identifier> channels) {
        unregister(server, handler, channels);
    }
}
