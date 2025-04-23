package io.izzel.arclight.fabric.mod.plugin.messaging;

import io.izzel.arclight.common.mod.server.ArclightServer;
import io.izzel.arclight.common.mod.plugin.messaging.ArclightPluginChannel;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.impl.networking.PayloadTypeRegistryImpl;
import net.minecraft.resources.ResourceLocation;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.messaging.Messenger;
import org.bukkit.plugin.messaging.PluginMessageListenerRegistration;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ArclightFabricMessaging {

    public static ArclightPluginChannel<? extends FabricPayloadHandler> setupChannel(Messenger messenger, ResourceLocation location, Set<PluginMessageListenerRegistration> incoming, Set<Plugin> outgoing) {
        if (verifyChannel(location, incoming, outgoing)) {
            var channel = new ArclightPluginChannel<>(messenger, ArclightFabricPayloadHandler::new, location, incoming, outgoing);
            PayloadTypeRegistry.configurationS2C().register(channel.getType(), channel.getStreamCodec());
            PayloadTypeRegistry.configurationC2S().register(channel.getType(), channel.getStreamCodec());
            PayloadTypeRegistry.playS2C().register(channel.getType(), channel.getStreamCodec());
            PayloadTypeRegistry.playC2S().register(channel.getType(), channel.getStreamCodec());
            ServerPlayNetworking.registerGlobalReceiver(channel.getType(), channel.getChannelHandler());
            return channel;
        } else {
            return new ArclightPluginChannel<>(messenger, ArclightFabricPayloadDestroyer::new, location, incoming, outgoing);
        }
    }

    private static boolean verifyChannel(ResourceLocation location, Set<PluginMessageListenerRegistration> incoming, Set<Plugin> outgoing) {
        for (var protocol : ArclightPluginChannel.PROTOCOLS) {
            var s2c = PayloadTypeRegistryImpl.PLAY_S2C.get(location);
            if (s2c != null) {
                var pluginList = Stream.concat(outgoing.stream(), incoming.stream().map(PluginMessageListenerRegistration::getPlugin))
                        .distinct()
                        .map(Plugin::getName)
                        .collect(Collectors.joining(", ", "[", "]"));
                ArclightServer.LOGGER.error("Attempting to register a channel that has already been registered by Fabric!");
                ArclightServer.LOGGER.error("Channel conflict: {}, in protocol: {}", location, protocol);
                ArclightServer.LOGGER.error("Registered by plugin(s): {}", pluginList);
                ArclightServer.LOGGER.error("This channel will be ignored for the rest of the time!");
                return false;
            }
        }
        return true;
    }
}
