package io.izzel.arclight.neoforge.mod.plugin.messaging;

import io.izzel.arclight.common.mod.plugin.messaging.ArclightPluginChannel;
import io.izzel.arclight.common.mod.plugin.messaging.ChannelDirection;
import io.izzel.arclight.common.mod.server.ArclightServer;
import io.izzel.arclight.neoforge.mixin.neoforge.NetworkRegistryAccessor;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.messaging.Messenger;
import org.bukkit.plugin.messaging.PluginMessageListenerRegistration;

import java.util.Map;
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

    public static ArclightPluginChannel<? extends NeoforgePayloadHandler> setupChannel(Messenger messenger, Identifier location, Set<PluginMessageListenerRegistration> incoming, Set<Plugin> outgoing) {
        if (verifyChannel(location, incoming, outgoing)) {
            return new ArclightPluginChannel<>(messenger, ArclightNfPayloadHandler::new, location, incoming, outgoing);
        } else {
            return new ArclightPluginChannel<>(messenger, ArclightNfPayloadDestroyer::new, location, incoming, outgoing);
        }
    }

    public static boolean verifyChannel(Identifier location, Set<PluginMessageListenerRegistration> incoming, Set<Plugin> outgoing) {
        for (var protocol : ArclightPluginChannel.PROTOCOLS) {
            var known = NetworkRegistryAccessor.getRegistration().get(protocol).get(location);
            var builtin = NetworkRegistryAccessor.getBuiltinPayload().get(location);
            if (known != null || builtin != null) {
                var pluginList = Stream.concat(outgoing.stream(), incoming.stream().map(PluginMessageListenerRegistration::getPlugin))
                        .distinct()
                        .map(Plugin::getName)
                        .collect(Collectors.joining(", ", "[", "]"));
                ArclightServer.LOGGER.error("Attempting to register a channel that has already been registered by NeoForge!");
                ArclightServer.LOGGER.error("Channel conflict: {}, in protocol: {}", location, protocol);
                ArclightServer.LOGGER.error("Registered by plugin(s): {}", pluginList);
                if (known != null) {
                    ArclightServer.LOGGER.error("Registered by mod version: {}", known.version());
                }
                ArclightServer.LOGGER.error("This channel will be ignored for the rest of the time!");
                return false;
            }
        }
        return true;
    }

    public static void updateChannel(ArclightPluginChannel<ArclightNfPayloadHandler> channel) {
        final var location = channel.getChannel();
        final var handler = channel.getChannelHandler();
        for (var protocol : ArclightPluginChannel.PROTOCOLS) {
            var map = NetworkRegistryAccessor.getRegistration().get(protocol);
            if (channel.getDirection() != getFlowFromRegistration(map.get(location))) {
                final var registration = createRegistration(channel);
                if (registration == null) {
                    map.remove(location);
                    removeHandlers(protocol, location);
                } else {
                    map.put(location, registration);
                    registerHandlers(protocol, location, handler, channel.getDirection());
                }
            }
        }
    }

    private static void registerHandlers(ConnectionProtocol protocol, Identifier location, IPayloadHandler<?> handler, ChannelDirection direction) {
        if (direction.hasIncoming()) {
            handlerMap(NetworkRegistryAccessor.getServerboundHandlers(), protocol).put(location, handler);
        }
        if (direction.hasOutgoing()) {
            handlerMap(NetworkRegistryAccessor.getClientboundHandlers(), protocol).put(location, handler);
        }
    }

    private static void removeHandlers(ConnectionProtocol protocol, Identifier location) {
        handlerMap(NetworkRegistryAccessor.getServerboundHandlers(), protocol).remove(location);
        handlerMap(NetworkRegistryAccessor.getClientboundHandlers(), protocol).remove(location);
    }

    private static Map<Identifier, IPayloadHandler<?>> handlerMap(Map<ConnectionProtocol, Map<Identifier, IPayloadHandler<?>>> root, ConnectionProtocol protocol) {
        return root.computeIfAbsent(protocol, ignored -> new java.util.concurrent.ConcurrentHashMap<>());
    }

    private static ChannelDirection getFlowFromRegistration(PayloadRegistration<?> registration) {
        if (registration == null) {
            return ChannelDirection.NONE;
        } else if (registration.flow().isEmpty()) {
            return ChannelDirection.BIDIRECTIONAL;
        } else {
            final var flow = registration.flow().get();
            if (flow == PacketFlow.SERVERBOUND) {
                return ChannelDirection.INCOMING;
            } else {
                return ChannelDirection.OUTGOING;
            }
        }
    }

    public static PayloadRegistration<?> createRegistration(ArclightPluginChannel<ArclightNfPayloadHandler> channel) {
        var direction = channel.getDirection();
        if (direction.bitmap == 0) {
            return null;
        }
        var type = channel.getType();
        var codec = channel.getStreamCodec();
        var flow = channel.getDirection().flow;
        var version = ArclightNfMessaging.ARCLIGHT_CUSTOM_CHANNEL_VERSION;

        return new PayloadRegistration<>(type, codec, ArclightPluginChannel.PROTOCOLS, Optional.ofNullable(flow), version, true);
    }
}
