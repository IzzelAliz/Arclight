package io.izzel.arclight.common.bridge.core.network;

import com.mojang.authlib.properties.Property;
import io.izzel.arclight.common.mod.util.ConnectionCookieRequests;
import io.izzel.arclight.common.mod.util.ConnectionResourcePackCallbacks;
import org.jetbrains.annotations.Nullable;

import java.net.SocketAddress;
import java.util.UUID;

public interface ConnectionBridge {

    java.net.SocketAddress bridge$getRawAddress();

    int bridge$getProtocolVersion();

    java.net.InetSocketAddress bridge$getVirtualHost();

    void bridge$setHandshakeMetadata(int protocolVersion, java.net.InetSocketAddress virtualHost);

    @org.jetbrains.annotations.Nullable
    org.bukkit.event.player.PlayerResourcePackStatusEvent.Status bridge$getResourcePackStatus();

    void bridge$setResourcePackStatus(org.bukkit.event.player.PlayerResourcePackStatusEvent.Status status);

    @Nullable
    String bridge$getClientBrandName();

    void bridge$setClientBrandName(@Nullable String brand);

    ConnectionCookieRequests bridge$getCookieRequests();

    ConnectionResourcePackCallbacks bridge$getResourcePackCallbacks();

    UUID bridge$getSpoofedUUID();

    void bridge$setSpoofedUUID(UUID spoofedUUID);

    Property[] bridge$getSpoofedProfile();

    void bridge$setSpoofedProfile(Property[] spoofedProfile);

    String bridge$getHostname();

    void bridge$setHostname(String hostname);
}
