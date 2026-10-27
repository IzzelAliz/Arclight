package io.izzel.arclight.common.mixin.core.network;

import com.mojang.authlib.properties.Property;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import io.izzel.arclight.common.bridge.core.network.ConnectionBridge;
import io.izzel.arclight.common.mod.util.ConnectionCookieRequests;
import io.izzel.arclight.common.mod.util.ConnectionResourcePackCallbacks;
import io.netty.channel.ChannelFuture;
import net.minecraft.network.Connection;
import net.minecraft.network.DisconnectionDetails;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Queue;
import java.util.UUID;
import java.util.function.Consumer;

@Mixin(Connection.class)
public class ConnectionMixin implements ConnectionBridge {

    @Shadow public boolean disconnectionHandled;
    @Shadow public io.netty.channel.Channel channel;

    @Override
    public java.net.SocketAddress bridge$getRawAddress() {
        return this.channel.remoteAddress();
    }
    @Shadow @Final private Queue<Consumer<Connection>> pendingActions;
    public java.util.UUID spoofedUUID;
    public com.mojang.authlib.properties.Property[] spoofedProfile;
    public String hostname;
    public int protocolVersion = -1;
    public java.net.InetSocketAddress virtualHost;

    @Override
    public int bridge$getProtocolVersion() {
        return this.protocolVersion;
    }

    @Override
    public java.net.InetSocketAddress bridge$getVirtualHost() {
        return this.virtualHost;
    }

    @Override
    public void bridge$setHandshakeMetadata(int protocolVersion, java.net.InetSocketAddress virtualHost) {
        this.protocolVersion = protocolVersion;
        this.virtualHost = virtualHost;
    }

    public volatile org.bukkit.event.player.PlayerResourcePackStatusEvent.Status resourcePackStatus;

    @Override
    public org.bukkit.event.player.PlayerResourcePackStatusEvent.Status bridge$getResourcePackStatus() {
        return this.resourcePackStatus;
    }

    @Override
    public void bridge$setResourcePackStatus(org.bukkit.event.player.PlayerResourcePackStatusEvent.Status status) {
        this.resourcePackStatus = status;
    }

    @Unique private volatile @Nullable String arclight$clientBrandName;

    @Override
    @Nullable
    public String bridge$getClientBrandName() {
        return this.arclight$clientBrandName;
    }

    @Override
    public void bridge$setClientBrandName(@Nullable String brand) {
        this.arclight$clientBrandName = brand;
    }

    @Unique private final ConnectionCookieRequests arclight$cookieRequests = new ConnectionCookieRequests();

    @Override
    public ConnectionCookieRequests bridge$getCookieRequests() {
        return this.arclight$cookieRequests;
    }

    @Unique private final ConnectionResourcePackCallbacks arclight$resourcePackCallbacks = new ConnectionResourcePackCallbacks();

    @Override
    public ConnectionResourcePackCallbacks bridge$getResourcePackCallbacks() {
        return this.arclight$resourcePackCallbacks;
    }

    @Override
    public UUID bridge$getSpoofedUUID() {
        return spoofedUUID;
    }

    @Override
    public void bridge$setSpoofedUUID(UUID spoofedUUID) {
        this.spoofedUUID = spoofedUUID;
    }

    @Override
    public Property[] bridge$getSpoofedProfile() {
        return spoofedProfile;
    }

    @Override
    public void bridge$setSpoofedProfile(Property[] spoofedProfile) {
        this.spoofedProfile = spoofedProfile;
    }

    @Override
    public String bridge$getHostname() {
        return hostname;
    }

    @Override
    public void bridge$setHostname(String hostname) {
        this.hostname = hostname;
    }

    @Redirect(method = "disconnect(Lnet/minecraft/network/DisconnectionDetails;)V", at = @At(value = "INVOKE", target = "Lio/netty/channel/ChannelFuture;awaitUninterruptibly()Lio/netty/channel/ChannelFuture;"))
    private ChannelFuture arclight$noAwaitClose(ChannelFuture future) {
        // Disconnect can run on the event loop; retain close and the native details tail.
        return future;
    }

    @Inject(method = "channelInactive(Lio/netty/channel/ChannelHandlerContext;)V", at = @At("HEAD"))
    private void arclight$closeConnectionRequests(CallbackInfo ci) {
        // Only the native channel's terminal lifecycle, never listener handoff or a cancellable kick.
        this.arclight$resourcePackCallbacks.close();
        this.arclight$cookieRequests.close();
    }

    @Inject(method = "handleDisconnection", at = @At(value = "FIELD", target = "Lnet/minecraft/network/Connection;disconnectionHandled:Z", opcode = Opcodes.PUTFIELD, shift = At.Shift.AFTER))
    private void arclight$markFirstDisconnection(CallbackInfo ci, @Share("arclight$firstDisconnection") LocalBooleanRef firstDisconnection) {
        firstDisconnection.set(true);
    }

    @Inject(method = "handleDisconnection", at = @At("RETURN"))
    private void arclight$clearDisconnectedQueue(CallbackInfo ci, @Share("arclight$firstDisconnection") LocalBooleanRef firstDisconnection) {
        // Only the first closed branch's normal tail; exceptions retain the queue.
        if (firstDisconnection.get()) {
            this.pendingActions.clear();
        }
    }

    @Inject(method = "handleDisconnection", at = @At("HEAD"), cancellable = true)
    private void arclight$noDisconnectTwiceWarn(CallbackInfo ci) {
        if (disconnectionHandled) {
            ci.cancel();
        }
    }
}
