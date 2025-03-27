package io.izzel.arclight.fabric.mod.event;

import io.izzel.arclight.common.bridge.core.network.common.ServerCommonPacketListenerBridge;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

import java.util.List;

public class S2CPlayNConfigChannelHandler implements S2CPlayChannelEvents.Register, S2CPlayChannelEvents.Unregister, S2CConfigurationChannelEvents.Register, S2CConfigurationChannelEvents.Unregister {

    public static void register() {
        var handler = new S2CPlayNConfigChannelHandler();
        S2CPlayChannelEvents.REGISTER.register(handler);
        S2CPlayChannelEvents.UNREGISTER.register(handler);
        S2CConfigurationChannelEvents.REGISTER.register(handler);
        S2CConfigurationChannelEvents.UNREGISTER.register(handler);
    }

    private void register(MinecraftServer server, ServerCommonPacketListenerImpl listener, List<ResourceLocation> channels) {
        server.executeIfPossible(() -> {
            var craftbukkit = ((ServerCommonPacketListenerBridge) listener).bridge$getCraftPlayer();
            for (var location : channels) {
                craftbukkit.addChannel(location.toString());
            }
        });
    }

    private void unregister(MinecraftServer server, ServerCommonPacketListenerImpl listener, List<ResourceLocation> channels) {
        server.executeIfPossible(() -> {
            var craftbukkit = ((ServerCommonPacketListenerBridge) listener).bridge$getCraftPlayer();
            for (var location : channels) {
                craftbukkit.removeChannel(location.toString());
            }
        });
    }

    @Override
    public void onChannelRegister(ServerGamePacketListenerImpl handler, PacketSender sender, MinecraftServer server, List<ResourceLocation> channels) {
        register(server, handler, channels);
    }

    @Override
    public void onChannelUnregister(ServerGamePacketListenerImpl handler, PacketSender sender, MinecraftServer server, List<ResourceLocation> channels) {
        unregister(server, handler, channels);
    }

    @Override
    public void onChannelRegister(ServerConfigurationPacketListenerImpl handler, PacketSender sender, MinecraftServer server, List<ResourceLocation> channels) {
        register(server, handler, channels);
    }

    @Override
    public void onChannelUnregister(ServerConfigurationPacketListenerImpl handler, PacketSender sender, MinecraftServer server, List<ResourceLocation> channels) {
        unregister(server, handler, channels);
    }
}
