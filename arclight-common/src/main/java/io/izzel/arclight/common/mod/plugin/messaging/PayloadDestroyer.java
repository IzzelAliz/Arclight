package io.izzel.arclight.common.mod.plugin.messaging;

import io.izzel.arclight.common.mod.server.ArclightServer;
import org.bukkit.craftbukkit.v.entity.CraftPlayer;
import org.bukkit.plugin.Plugin;

public interface PayloadDestroyer extends PluginChannelHandler {

    default void sendCustomPayload(Plugin src, CraftPlayer dst, byte[] data) {
        ArclightServer.LOGGER.debug("Ignoring sendCustomPayload for channel {} due to conflict with mod channel.", channel().getChannel());
    }

    @Override
    default void updateChannel() {}
}
