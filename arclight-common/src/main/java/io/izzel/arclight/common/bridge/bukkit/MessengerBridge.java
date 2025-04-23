package io.izzel.arclight.common.bridge.bukkit;

import io.izzel.arclight.common.mod.plugin.messaging.ArclightPluginChannel;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import net.minecraft.resources.ResourceLocation;
import org.bukkit.craftbukkit.v.entity.CraftPlayer;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.messaging.PluginMessageListenerRegistration;

import java.util.Set;

public interface MessengerBridge {
    Object2BooleanOpenHashMap<String> valid = new Object2BooleanOpenHashMap<>();

    ArclightPluginChannel<?> bridge$setupChannel(ResourceLocation channel, Set<PluginMessageListenerRegistration> incoming, Set<Plugin> outgoing);
    default void bridge$updateChannel(ArclightPluginChannel<?> channel) {}

    void bridge$sendCustomPayload(Plugin src, CraftPlayer dst, ResourceLocation location, byte[] data);
    void bridge$registerAnonymousOutgoing(ResourceLocation location);
    ArclightPluginChannel<?> bridge$getAndCheckCrossSend(Plugin src, ResourceLocation channel);
    void bridge$checkUnsafeSend(Plugin src, ResourceLocation channel);
}
