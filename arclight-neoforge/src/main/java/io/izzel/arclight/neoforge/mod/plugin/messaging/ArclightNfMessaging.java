package io.izzel.arclight.neoforge.mod.plugin.messaging;

import io.izzel.arclight.common.mod.plugin.messaging.ArclightPluginChannel;
import io.izzel.arclight.common.mod.plugin.messaging.ArclightRawPayload;
import io.izzel.arclight.common.mod.server.ArclightServer;
import io.izzel.arclight.neoforge.mixin.neoforge.NetworkRegistryAccessor;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistration;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * CHANNEL_INITIALIZER act as hook between common and SPI.
 * ARCLIGHT_CUSTOM_CHANNEL_VERSION act as a mark for bypassing channel validation.
 * Use its identity to prevent any possibility of conflict.
 */
public class ArclightNfMessaging {
    @SuppressWarnings("StringOperationCanBeSimplified")
    public static String ARCLIGHT_CUSTOM_CHANNEL_VERSION = new String("arclight:custom/bukkit");
    public static Consumer<ArclightPluginChannel> CHANNEL_INITIALIZER;

    public static final Map<ResourceLocation, IPayloadHandler<ArclightRawPayload>> PLUGIN_CHANNELS = new HashMap<>();

    public static void updateChannel(ArclightPluginChannel channel) {
        var entry = PLUGIN_CHANNELS.get(channel.getChannel());
        var location = channel.getChannel();
        if (entry == null) {
            if (verifyChannel(channel)) {
                var registration = createRegistration(channel);
                for (var protocol : channel.getProtocols()) {
                    var map = NetworkRegistryAccessor.getRegistration().get(protocol);
                    map.put(location, registration);
                }
                PLUGIN_CHANNELS.put(location, (ArclightNfPayloadHandler) registration.handler());
            } else {
                PLUGIN_CHANNELS.put(location, new ArclightNfPayloadDestroyer());
            }
        }
    }

    public static boolean verifyChannel(ArclightPluginChannel channel) {
        var protocols = channel.getProtocols();
        var location = channel.getChannel();
        for (var protocol : protocols) {
            var known = NetworkRegistryAccessor.getRegistration().get(protocol).get(location);
            if (known != null) {
                var pluginList = channel.getOutgoing()
                        .stream()
                        .map(Plugin::getName)
                        .collect(Collectors.joining(", ", "[", "]"));
                ArclightServer.LOGGER.error("Plugin is attempting to register a channel that has already been registered by NeoForge!");
                ArclightServer.LOGGER.error("Channel conflict: {}, in protocol: {}", location, protocol);
                ArclightServer.LOGGER.error("Registered by plugin(s): {}", pluginList);
                ArclightServer.LOGGER.error("Registered by mod version: {}", known.version());
                ArclightServer.LOGGER.error("This channel will be ignored for the rest of the time!");
                return false;
            }
        }
        return true;
    }

    public static PayloadRegistration<?> createRegistration(ArclightPluginChannel channel) {
        var direction = channel.getDirection();
        if (direction.bitmap == 0) {
            return null;
        }
        var protocols = channel.getProtocols();
        var handler = new ArclightNfPayloadHandler(channel);
        var type = channel.getType();
        var codec = channel.getStreamCodec();
        var flow = direction.flow;
        var version = ArclightNfMessaging.ARCLIGHT_CUSTOM_CHANNEL_VERSION;

        return new PayloadRegistration<>(type, codec, handler, protocols, Optional.ofNullable(flow), version, true);
    }
}
