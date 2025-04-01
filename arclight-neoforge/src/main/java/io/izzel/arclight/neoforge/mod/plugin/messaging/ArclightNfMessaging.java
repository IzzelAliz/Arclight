package io.izzel.arclight.neoforge.mod.plugin.messaging;

import io.izzel.arclight.common.mod.plugin.messaging.ArclightPluginChannel;
import io.izzel.arclight.common.mod.server.ArclightServer;
import io.izzel.arclight.neoforge.mixin.neoforge.NetworkRegistryAccessor;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.registration.PayloadRegistration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.messaging.Messenger;
import org.bukkit.plugin.messaging.PluginMessageListenerRegistration;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * ARCLIGHT_CUSTOM_CHANNEL_VERSION act as a mark for bypassing channel validation.
 * Use its identity to prevent any possibility of conflict.
 */
public class ArclightNfMessaging {
    @SuppressWarnings("StringOperationCanBeSimplified")
    public static String ARCLIGHT_CUSTOM_CHANNEL_VERSION = new String("arclight:custom/bukkit");

    public static ArclightPluginChannel<? extends NeoforgePayloadHandler> setupChannel(Messenger messenger, ResourceLocation location, Set<PluginMessageListenerRegistration> incoming, Set<Plugin> outgoing) {
        if (verifyChannel(location, incoming, outgoing)) {
            var channel = new ArclightPluginChannel<>(messenger, ArclightNfPayloadHandler::new, location, incoming, outgoing);
            var registration = createRegistration(channel);
            for (var protocol : ArclightPluginChannel.PROTOCOLS) {
                var map = NetworkRegistryAccessor.getRegistration().get(protocol);
                map.put(location, registration);
            }
            return channel;
        } else {
            return new ArclightPluginChannel<>(messenger, ArclightNfPayloadDestroyer::new, location, incoming, outgoing);
        }
    }

    public static boolean verifyChannel(ResourceLocation location, Set<PluginMessageListenerRegistration> incoming, Set<Plugin> outgoing) {
        for (var protocol : ArclightPluginChannel.PROTOCOLS) {
            var known = NetworkRegistryAccessor.getRegistration().get(protocol).get(location);
            if (known != null) {
                var pluginList = Stream.concat(outgoing.stream(), incoming.stream().map(PluginMessageListenerRegistration::getPlugin))
                        .distinct()
                        .map(Plugin::getName)
                        .collect(Collectors.joining(", ", "[", "]"));
                ArclightServer.LOGGER.error("Attempting to register a channel that has already been registered by NeoForge!");
                ArclightServer.LOGGER.error("Channel conflict: {}, in protocol: {}", location, protocol);
                ArclightServer.LOGGER.error("Registered by plugin(s): {}", pluginList);
                ArclightServer.LOGGER.error("Registered by mod version: {}", known.version());
                ArclightServer.LOGGER.error("This channel will be ignored for the rest of the time!");
                return false;
            }
        }
        return true;
    }

    public static PayloadRegistration<?> createRegistration(ArclightPluginChannel<ArclightNfPayloadHandler> channel) {
        var direction = channel.getDirection();
        if (direction.bitmap == 0) {
            return null;
        }
        var handler = channel.getChannelHandler();
        var type = channel.getType();
        var codec = channel.getStreamCodec();
        var flow = direction.flow;
        var version = ArclightNfMessaging.ARCLIGHT_CUSTOM_CHANNEL_VERSION;

        return new PayloadRegistration<>(type, codec, handler, ArclightPluginChannel.PROTOCOLS, Optional.ofNullable(flow), version, true);
    }
}
