package io.izzel.arclight.common.mixin.bukkit;

import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.SetMultimap;
import io.izzel.arclight.common.bridge.bukkit.MessengerBridge;
import io.izzel.arclight.common.mod.server.ArclightServer;
import io.izzel.arclight.common.mod.plugin.messaging.ArclightPluginChannel;
import net.minecraft.resources.ResourceLocation;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.messaging.Messenger;
import org.bukkit.plugin.messaging.PluginMessageListenerRegistration;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.bukkit.plugin.messaging.StandardMessenger;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.*;
import java.util.function.Consumer;

@Mixin(value = StandardMessenger.class, remap = false)
public abstract class StandardMessengerMixin implements Messenger, MessengerBridge {

    @Shadow @Final private Map<String, Set<Plugin>> outgoingByChannel;

    @Shadow @Final private Map<String, Set<PluginMessageListenerRegistration>> incomingByChannel;

    @Shadow @Final private Map<Plugin, Set<String>> outgoingByPlugin;

    @Unique
    private Map<ResourceLocation, ArclightPluginChannel> arclight$registry;

    @Unique
    private SetMultimap<Plugin, ResourceLocation> crossSend;

    @Unique
    private SetMultimap<Plugin, ResourceLocation> unsafeSend;

    @Unique
    private Consumer<ArclightPluginChannel> updater;

    @ModifyConstant(
            method = "validateAndCorrectChannel",
            constant = @Constant(intValue = Messenger.MAX_CHANNEL_SIZE)
    )
    private static int arclight$modifyMaxChannelSize(int original) {
        return 256;
    }

    @Override
    public void bridge$setChannelUpdater(Consumer<ArclightPluginChannel> updater) {
        this.updater = updater;
    }

    @Override
    public ArclightPluginChannel bridge$getAndCheckCrossSend(Plugin src, ResourceLocation channel) {
        var arclight = this.arclight$registry.get(channel);
        if (!arclight.getOutgoing().contains(src)) {
            boolean first;
            synchronized (this.crossSend) {
                first = this.crossSend.put(src, channel);
            }
            if (first) {
                ArclightServer.LOGGER.warn("A plugin is sending message on a channel that's registered as outgoing by other plugins but itself!");
                ArclightServer.LOGGER.warn("Plugin: [{}], on channel: {}", src.getDescription().getFullName(), channel);
                ArclightServer.LOGGER.warn("This warning will only be displayed once for every plugin and channel.");
            }
        }
        return arclight;
    }

    @Override
    public boolean bridge$checkUnsafeSend(Plugin src, ResourceLocation channel) {
        if (outgoingByChannel.containsKey(channel.toString())) {
            return true;
        }
        boolean first;
        synchronized (this.unsafeSend) {
            first = this.unsafeSend.put(src, channel);
        }
        if (first) {
            ArclightServer.LOGGER.error("A plugin is sending message on a channel that's not registered as outgoing by any plugin!");
            ArclightServer.LOGGER.error("Plugin: [{}], on channel: {}", src.getDescription().getFullName(), channel);
            ArclightServer.LOGGER.error("This detailed error message will only be displayed once for every plugin and channel!");
        }
        ArclightServer.LOGGER.error("Plugin [{}] is sending message on an unregistered outgoing channel {}, aborting!", src.getDescription().getFullName(), channel);
        return false;
    }

    @Unique
    private void arclight$updateChannel(String id) {
        var location = ResourceLocation.tryParse(id);
        if (location != null) {
            var channel = arclight$registry.computeIfAbsent(location, it -> {
                var inByChannel = incomingByChannel.computeIfAbsent(id, k -> new HashSet<>());
                var outByChannel = outgoingByChannel.computeIfAbsent(id, k -> new HashSet<>());
                return new ArclightPluginChannel((StandardMessenger)(Messenger) this, it, inByChannel, outByChannel);
            });
            Objects.requireNonNull(updater, "Channel updater cannot be null").accept(channel);
        }
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void arclight$init(CallbackInfo ci) {
        arclight$registry = new HashMap<>();
        crossSend = MultimapBuilder.hashKeys().hashSetValues().build();
        unsafeSend = MultimapBuilder.hashKeys().hashSetValues().build();
    }

    @Inject(method = "addToOutgoing", at = @At("TAIL"))
    private void arclight$registerOut(Plugin plugin, String id, CallbackInfo ci) {
        arclight$updateChannel(id);
    }

    @Inject(method = "removeFromOutgoing(Lorg/bukkit/plugin/Plugin;Ljava/lang/String;)V", at = @At("TAIL"))
    private void arclight$unregisterOut(Plugin plugin, String id, CallbackInfo ci) {
        arclight$updateChannel(id);
    }

    @Inject(method = "addToIncoming", at = @At("TAIL"))
    private void arclight$registerIn(PluginMessageListenerRegistration registration, CallbackInfo ci) {
        arclight$updateChannel(registration.getChannel());
    }

    @Inject(method = "removeFromIncoming(Lorg/bukkit/plugin/messaging/PluginMessageListenerRegistration;)V", at = @At("TAIL"))
    private void arclight$unregisterIn(PluginMessageListenerRegistration registration, CallbackInfo ci) {
        arclight$updateChannel(registration.getChannel());
    }

    @Inject(method = "validateAndCorrectChannel", at = @At("TAIL"))
    private static void arclight$enhancedValidation(String channel, CallbackInfoReturnable<String> cir) {
        var corrected = cir.getReturnValue();
        var namespace = corrected.substring(0, corrected.indexOf(':'));
        var path = corrected.substring(corrected.indexOf(':') + 1);
        if (!ResourceLocation.isValidNamespace(namespace) || !ResourceLocation.isValidPath(path)) {
            ArclightServer.LOGGER.warn("Channel name is malformed and impossible to register: {}", corrected);
            ArclightServer.LOGGER.warn("Related functionality cannot be guaranteed!");
            ArclightServer.LOGGER.warn("This message will only be displayed once for this name!");
        }
    }
}