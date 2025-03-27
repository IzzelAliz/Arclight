package io.izzel.arclight.fabric.mod.plugin.messaging;

import io.izzel.arclight.common.mod.server.ArclightServer;
import io.izzel.arclight.common.mod.plugin.messaging.ArclightPluginChannel;
import io.izzel.arclight.common.mod.plugin.messaging.ArclightRawPayload;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.impl.networking.PayloadTypeRegistryImpl;
import net.minecraft.resources.ResourceLocation;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class ArclightFabricMessaging {
    private static final Map<ResourceLocation, ServerPlayNetworking.PlayPayloadHandler<ArclightRawPayload>> PLUGIN_CHANNELS = new HashMap<>();

    public static void initChannel(ArclightPluginChannel channel) {
        var entry = PLUGIN_CHANNELS.get(channel.getChannel());
        var location = channel.getChannel();
        if (entry == null) {
            if (verifyChannel(channel)) {
                var registration = new ArclightFabricPayloadHandler(channel);
                PayloadTypeRegistry.playS2C().register(channel.getType(), channel.getStreamCodec());
                ServerPlayNetworking.registerGlobalReceiver(channel.getType(), registration);
                PLUGIN_CHANNELS.put(location, registration);
            } else {
                PLUGIN_CHANNELS.put(location, new ArclightFabricPayloadDestroyer());
            }
        }
    }

    private static boolean verifyChannel(ArclightPluginChannel channel) {
        var protocols = channel.getProtocols();
        var location = channel.getChannel();
        for (var protocol : protocols) {
            var s2c = PayloadTypeRegistryImpl.PLAY_S2C.get(location);
            if (s2c != null) {
                var pluginList = channel.getOutgoing()
                        .stream()
                        .map(Plugin::getName)
                        .collect(Collectors.joining(", ", "[", "]"));
                ArclightServer.LOGGER.error("Plugin is attempting to register a channel that has already been registered by Fabric!");
                ArclightServer.LOGGER.error("Channel conflict: {}, in protocol: {}", location, protocol);
                ArclightServer.LOGGER.error("Registered by plugin(s): {}", pluginList);
                ArclightServer.LOGGER.error("This channel will be ignored for the rest of the time!");
                return false;
            }
        }
        return true;
    }
}
