package io.izzel.arclight.neoforge.mixin.bukkit;

import io.izzel.arclight.common.bridge.bukkit.MessengerBridge;
import io.izzel.arclight.common.mod.plugin.messaging.ArclightPluginChannel;
import io.izzel.arclight.neoforge.mod.plugin.messaging.ArclightNfMessaging;
import net.minecraft.resources.ResourceLocation;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.messaging.Messenger;
import org.bukkit.plugin.messaging.PluginMessageListenerRegistration;
import org.bukkit.plugin.messaging.StandardMessenger;
import org.spongepowered.asm.mixin.Mixin;

import java.util.Set;

@Mixin(value = StandardMessenger.class, remap = false)
public abstract class StandardMessengerMixin_NeoForge implements Messenger, MessengerBridge {

    @Override
    public ArclightPluginChannel<?> arclight$setupChannel(ResourceLocation channel, Set<PluginMessageListenerRegistration> incoming, Set<Plugin> outgoing) {
        return ArclightNfMessaging.setupChannel(this, channel, incoming, outgoing);
    }
}
