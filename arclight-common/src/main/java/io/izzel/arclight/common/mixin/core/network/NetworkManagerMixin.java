package io.izzel.arclight.common.mixin.core.network;

import com.mojang.authlib.properties.Property;
import io.izzel.arclight.common.bridge.network.NetworkManagerBridge;
import io.netty.channel.Channel;
import net.minecraft.network.NetworkManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.net.SocketAddress;
import java.util.UUID;

@Mixin(NetworkManager.class)
public class NetworkManagerMixin implements NetworkManagerBridge {

    @Shadow public Channel channel;
    public java.util.UUID spoofedUUID;
    public com.mojang.authlib.properties.Property[] spoofedProfile;

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

    public SocketAddress getRawAddress() {
        return this.channel.remoteAddress();
    }

    @Override
    public SocketAddress bridge$getRawAddress() {
        return getRawAddress();
    }
}
