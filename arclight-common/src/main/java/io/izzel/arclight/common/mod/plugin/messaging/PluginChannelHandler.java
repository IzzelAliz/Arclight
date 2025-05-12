package io.izzel.arclight.common.mod.plugin.messaging;

import org.bukkit.craftbukkit.v.entity.CraftPlayer;
import org.bukkit.plugin.Plugin;

public interface PluginChannelHandler {
    ArclightPluginChannel<?> channel();
    void updateChannel();
    void sendCustomPayload(Plugin src, CraftPlayer dst, byte[] data);
}
