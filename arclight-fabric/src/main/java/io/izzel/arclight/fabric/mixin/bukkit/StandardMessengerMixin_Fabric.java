package io.izzel.arclight.fabric.mixin.bukkit;

import io.izzel.arclight.common.bridge.bukkit.MessengerBridge;
import io.izzel.arclight.common.mod.plugin.messaging.ArclightPluginChannel;
import io.izzel.arclight.fabric.mod.plugin.messaging.ArclightFabricMessaging;
import net.minecraft.resources.ResourceLocation;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.messaging.Messenger;
import org.bukkit.plugin.messaging.PluginMessageListenerRegistration;
import org.bukkit.plugin.messaging.StandardMessenger;
import org.spongepowered.asm.mixin.Mixin;

import java.util.Set;

@Mixin(value = StandardMessenger.class, remap = false)
public abstract class StandardMessengerMixin_Fabric implements Messenger, MessengerBridge {

    @Override
    public ArclightPluginChannel<?> arclight$setupChannel(ResourceLocation channel, Set<PluginMessageListenerRegistration> incoming, Set<Plugin> outgoing) {
        return ArclightFabricMessaging.setupChannel(this, channel, incoming, outgoing);
    }
}
