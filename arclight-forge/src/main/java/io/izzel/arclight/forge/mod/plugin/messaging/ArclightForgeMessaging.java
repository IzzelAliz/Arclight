package io.izzel.arclight.forge.mod.plugin.messaging;

import io.izzel.arclight.common.mod.plugin.messaging.ArclightPluginChannel;
import io.izzel.arclight.common.mod.server.ArclightServer;
import io.izzel.arclight.forge.mixin.forge.NetworkRegistryAccessor;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.Channel;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.NetworkRegistry;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.messaging.Messenger;
import org.bukkit.plugin.messaging.PluginMessageListenerRegistration;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ArclightForgeMessaging {

    public static final Channel.VersionTest ACCEPT_ALL = (status, version) -> true;

    public static ArclightPluginChannel<? extends ForgePayloadHandler> setupChannel(Messenger messenger, ResourceLocation location, Set<PluginMessageListenerRegistration> incoming, Set<Plugin> outgoing) {
        if (verifyChannel(location, incoming, outgoing)) {
            var channel = new ArclightPluginChannel<>(messenger, ArclightForgePayloadHandler::new, location, incoming, outgoing);
            var registration = channel.getChannelHandler();
            try {
                NetworkRegistryAccessor.setLock(false);
                var event = ChannelBuilder.named(location)
                        .serverAcceptedVersions(ACCEPT_ALL)
                        .optional()
                        .eventNetworkChannel();
                registration.initialize(event);
            } finally {
                NetworkRegistryAccessor.setLock(true);
            }
            return channel;
        } else {
            return new ArclightPluginChannel<>(messenger, ArclightForgePayloadDestroyer::new, location, incoming, outgoing);
        }
    }

    private static boolean verifyChannel(ResourceLocation location, Set<PluginMessageListenerRegistration> incoming, Set<Plugin> outgoing) {
        for (var protocol : ArclightPluginChannel.PROTOCOLS) {
            var registration = NetworkRegistry.findTarget(location);
            if (registration != null) {
                var pluginList = Stream.concat(outgoing.stream(), incoming.stream().map(PluginMessageListenerRegistration::getPlugin))
                        .distinct()
                        .map(Plugin::getName)
                        .collect(Collectors.joining(", ", "[", "]"));
                ArclightServer.LOGGER.error("Attempting to register a channel that has already been registered by Forge!");
                ArclightServer.LOGGER.error("Channel conflict: {}, in protocol: {}", location, protocol);
                ArclightServer.LOGGER.error("Registered by plugin(s): {}", pluginList);
                ArclightServer.LOGGER.error("This channel will be ignored for the rest of the time!");
                return false;
            }
        }
        return true;
    }
}
