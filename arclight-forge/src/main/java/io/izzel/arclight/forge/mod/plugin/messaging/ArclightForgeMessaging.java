package io.izzel.arclight.forge.mod.plugin.messaging;

import io.izzel.arclight.common.mod.plugin.messaging.ArclightPluginChannel;
import io.izzel.arclight.common.mod.server.ArclightServer;
import io.izzel.arclight.forge.mixin.forge.NetworkRegistryAccessor;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.Channel;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.SimpleChannel;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class ArclightForgeMessaging {

    public static final Channel.VersionTest ACCEPT_ALL = (status, version) -> true;

    public static final Map<ResourceLocation, ForgePayloadHandler> PLUGIN_CHANNELS = new HashMap<>();

    public static SimpleChannel getSimpleChannel(ResourceLocation location) {
        var entry = PLUGIN_CHANNELS.get(location);
        if (!(entry instanceof ArclightForgePayloadHandler handler)) {
            return null;
        }
        return handler.getSimpleChannel();
    }

    public static void updateChannel(ArclightPluginChannel channel) {
        var location = channel.getChannel();
        var entry = PLUGIN_CHANNELS.get(location);
        if (entry == null) {
            if (verifyChannel(channel)) {
                var registration = new ArclightForgePayloadHandler(channel);
                try {
                    NetworkRegistryAccessor.setLock(false);
                    var simple = ChannelBuilder.named(location)
                            .serverAcceptedVersions(ACCEPT_ALL)
                            .optional()
                            .simpleChannel();
                    registration.initialize(simple);
                } finally {
                    NetworkRegistryAccessor.setLock(true);
                }
                PLUGIN_CHANNELS.put(location, registration);
            } else {
                PLUGIN_CHANNELS.put(location, new ArclightForgePayloadDestroyer());
            }
        }

    }

    private static boolean verifyChannel(ArclightPluginChannel channel) {
        var protocols = channel.getProtocols();
        var location = channel.getChannel();
        for (var protocol : protocols) {
            var registration = NetworkRegistry.findTarget(location);
            if (registration != null) {
                var pluginList = channel.getOutgoing()
                        .stream()
                        .map(Plugin::getName)
                        .collect(Collectors.joining(", ", "[", "]"));
                ArclightServer.LOGGER.error("Plugin is attempting to register a channel that has already been registered by Forge!");
                ArclightServer.LOGGER.error("Channel conflict: {}, in protocol: {}", location, protocol);
                ArclightServer.LOGGER.error("Registered by plugin(s): {}", pluginList);
                ArclightServer.LOGGER.error("This channel will be ignored for the rest of the time!");
                return false;
            }
        }
        return true;
    }
}
