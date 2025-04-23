package io.izzel.arclight.common.mod.plugin.messaging;

import io.izzel.arclight.common.mod.ArclightConstants;
import io.izzel.arclight.common.mod.server.ArclightServer;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.bukkit.craftbukkit.v.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.messaging.Messenger;
import org.bukkit.plugin.messaging.PluginMessageListenerRegistration;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * Incoming and outgoing are views of Maps in StandardMessenger.
 * They are automatically synced with StandardMessenger.
 */
public class ArclightPluginChannel<T extends PluginChannelHandler> {

    public static final List<ConnectionProtocol> PROTOCOLS = List.of(ConnectionProtocol.CONFIGURATION, ConnectionProtocol.PLAY);
    private final Messenger messenger;
    private final CustomPacketPayload.Type<ArclightRawPayload> type;
    private final StreamCodec<? super FriendlyByteBuf, ArclightRawPayload> streamCodec;
    private final T handler;
    // Views start
    private final Set<PluginMessageListenerRegistration> incoming;
    private final Set<Plugin> outgoing;
    // Views end

    public ArclightPluginChannel(Messenger messenger, Function<ArclightPluginChannel<T>, T> factory, ResourceLocation channel, Set<PluginMessageListenerRegistration> incoming, Set<Plugin> outgoing) {
        this.messenger = messenger;
        this.type = ArclightRawPayload.getType(channel);
        this.streamCodec = RawPayload.channelCodec(this.type, ArclightConstants.MAX_C2S_CUSTOM_PAYLOAD_SIZE);
        this.handler = factory.apply(this);
        this.incoming = Collections.unmodifiableSet(incoming);
        this.outgoing = Collections.unmodifiableSet(outgoing);
    }

    public ChannelDirection getDirection() {
        if (incoming.isEmpty()) {
            if (outgoing.isEmpty()) {
                return ChannelDirection.NONE;
            } else {
                return ChannelDirection.OUTGOING;
            }
        } else {
            if (outgoing.isEmpty()) {
                return ChannelDirection.INCOMING;
            } else {
                return ChannelDirection.BIDIRECTIONAL;
            }
        }
    }

    public T getChannelHandler() {
        return handler;
    }

    public Set<Plugin> getOutgoing() {
        return outgoing;
    }

    public ResourceLocation getChannel() {
        return type.id();
    }

    public CustomPacketPayload.Type<ArclightRawPayload> getType() {
        return type;
    }

    public StreamCodec<? super FriendlyByteBuf, ArclightRawPayload> getStreamCodec() {
        return streamCodec;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public <B extends FriendlyByteBuf> StreamCodec<B, ArclightRawPayload> getCast() {
        // This is very OK for our implementation
        // ByteBuf is always an input argument
        return (StreamCodec) streamCodec;
    }

    public void dispatchMessage(Player src, byte[] message) {
        var fire = Set.copyOf(this.incoming);
        if (fire.isEmpty()) {
            ArclightServer.LOGGER.warn("Plugin channel {} has an incoming packet that has nowhere to dispatch.", type.id());
            return;
        }
        for (var listener : fire) {
            listener.getListener().onPluginMessageReceived(type.id().toString(), src, message);
        }
    }

    public void sendCustomPayload(Plugin src, CraftPlayer dst, byte[] data) {
        handler.sendCustomPayload(src, dst, data);
    }

    public Messenger getMessenger() {
        return messenger;
    }
}
