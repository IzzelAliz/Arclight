package io.izzel.arclight.common.mixin.core.server.network;

import com.destroystokyo.paper.event.player.PlayerHandshakeEvent;
import com.google.gson.Gson;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.authlib.properties.Property;
import io.izzel.arclight.common.bridge.core.network.ConnectionBridge;
import io.papermc.paper.adventure.PaperAdventure;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.handshake.ClientIntentionPacket;
import net.minecraft.network.protocol.login.ClientboundLoginDisconnectPacket;
import net.minecraft.server.network.ServerHandshakePacketListenerImpl;
import org.spigotmc.SpigotConfig;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.regex.Pattern;

@Mixin(ServerHandshakePacketListenerImpl.class)
public abstract class ServerHandshakeEventMixin {

    @Unique private static final Gson arclight$handshakeGson = new Gson();
    @Unique private static final Pattern arclight$handshakeHostPattern = Pattern.compile("[0-9a-f\\.:]{0,45}");

    @Shadow @Final private Connection connection;

    // Shares a method-local verdict with the existing legacy forwarding callback.
    // That callback must consume this namespace before this mixin is registered.
    @Inject(method = "beginLogin(Lnet/minecraft/network/protocol/handshake/ClientIntentionPacket;Z)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/network/Connection;setupInboundProtocol(Lnet/minecraft/network/ProtocolInfo;Lnet/minecraft/network/PacketListener;)V", shift = At.Shift.AFTER),
        order = 900, cancellable = true, require = 1, expect = 1, allow = 1)
    private void arclight$playerHandshake(ClientIntentionPacket originalPacket, boolean transfer, CallbackInfo ci,
                                          @Local(argsOnly = true) LocalRef<ClientIntentionPacket> packetRef,
                                          @Share(value = "handled", namespace = "arclight:player-handshake") LocalBooleanRef handled) {
        if (PlayerHandshakeEvent.getHandlerList().getRegisteredListeners().length == 0) {
            return;
        }

        SocketAddress remote = this.connection.address;
        String remoteHostname = remote instanceof InetSocketAddress socket
            ? socket.getHostString() : InetAddress.getLoopbackAddress().getHostAddress();
        PlayerHandshakeEvent event = new PlayerHandshakeEvent(originalPacket.hostName(), remoteHostname, !SpigotConfig.bungee);
        if (!event.callEvent()) {
            // Cancellation selects the existing forwarding path; it is not a login veto.
            return;
        }
        if (event.isFailed()) {
            var reason = PaperAdventure.asVanilla(event.failMessage());
            this.connection.send(new ClientboundLoginDisconnectPacket(reason));
            this.connection.disconnect(reason);
            ci.cancel();
            return;
        }

        ConnectionBridge bridge = (ConnectionBridge) this.connection;
        ClientIntentionPacket packet = originalPacket;
        if (event.getServerHostname() != null) {
            packet = new ClientIntentionPacket(originalPacket.protocolVersion(), event.getServerHostname(), originalPacket.port(), originalPacket.intention());
            packetRef.set(packet);
            bridge.bridge$setHostname(event.getServerHostname());
        }
        if (event.getSocketAddressHostname() != null) {
            this.connection.address = new InetSocketAddress(event.getSocketAddressHostname(), remote instanceof InetSocketAddress socket ? socket.getPort() : 0);
        }
        bridge.bridge$setSpoofedUUID(event.getUniqueId());
        bridge.bridge$setSpoofedProfile(arclight$handshakeGson.fromJson(event.getPropertiesJson(), Property[].class));
        handled.set(true);

        // Paper still rejects an unconsumed legacy forwarding envelope after an
        // accepted custom handshake. Never reinterpret cancellation as this path.
        String[] split = packet.hostName().split("\00");
        if ((split.length == 3 || split.length == 4) && arclight$handshakeHostPattern.matcher(split[1]).matches()) {
            var reason = net.minecraft.network.chat.Component.literal("Unknown data in login hostname, did you forget to enable BungeeCord in spigot.yml?");
            this.connection.send(new ClientboundLoginDisconnectPacket(reason));
            this.connection.disconnect(reason);
            ci.cancel();
        }
        // No login state or authentication/Velocity challenge is changed here.
    }
}
