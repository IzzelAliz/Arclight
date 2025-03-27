package io.izzel.arclight.common.bridge.bukkit;

import io.izzel.arclight.common.mod.plugin.messaging.ArclightPluginChannel;
import net.minecraft.resources.ResourceLocation;
import org.bukkit.craftbukkit.v.entity.CraftPlayer;
import org.bukkit.plugin.Plugin;

import java.util.function.Consumer;

public interface MessengerBridge {
    void bridge$setChannelUpdater(Consumer<ArclightPluginChannel> updater);
    void bridge$sendCustomPayload(Plugin src, CraftPlayer dst, ResourceLocation location, byte[] data);
    ArclightPluginChannel bridge$getAndCheckCrossSend(Plugin src, ResourceLocation channel);
    boolean bridge$checkUnsafeSend(Plugin src, ResourceLocation channel);
}
