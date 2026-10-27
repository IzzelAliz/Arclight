package io.izzel.arclight.common.mixin.core.network;

import com.destroystokyo.paper.event.player.PlayerConnectionCloseEvent;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.authlib.GameProfile;
import io.izzel.arclight.common.bridge.core.server.network.ServerLoginPacketListenerImplBridge;
import io.izzel.arclight.common.mixin.core.server.network.ServerLoginConnectionCloseAccessor;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Objects;

@Mixin(Connection.class)
public abstract class ConnectionCloseEventMixin {

    @Shadow public SocketAddress address;

    @ModifyExpressionValue(method = "handleDisconnection()V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/network/Connection;getPacketListener()Lnet/minecraft/network/PacketListener;"),
        require = 1, expect = 1, allow = 1)
    private PacketListener arclight$captureClosingListener(PacketListener listener,
                                                          @Share("arclight$closingListener") LocalRef<PacketListener> closingListener) {
        // This call exists only in the first closed-connection branch. Preserve
        // its result and capture before onDisconnect can replace the listener.
        closingListener.set(listener);
        return listener;
    }

    @Inject(method = "handleDisconnection()V", at = @At("RETURN"), order = 1100, require = 1)
    private void arclight$playerConnectionClosed(CallbackInfo ci,
                                                @Share("arclight$closingListener") LocalRef<PacketListener> closingListener) {
        // Run after the existing normal-return queue cleanup. A thrown native
        // onDisconnect/cleanup and duplicate/open/null-channel paths do not emit.
        PacketListener listener = closingListener.get();
        GameProfile profile;
        if (listener instanceof ServerCommonPacketListenerImpl commonListener) {
            profile = commonListener.getOwner();
        } else if (listener instanceof ServerLoginPacketListenerImpl loginListener) {
            var state = ((ServerLoginConnectionCloseAccessor) loginListener).arclight$getConnectionCloseState();
            switch (state) {
                case VERIFYING, WAITING_FOR_DUPE_DISCONNECT, PROTOCOL_SWITCHING, ACCEPTED ->
                    profile = Objects.requireNonNull(((ServerLoginPacketListenerImplBridge) loginListener).bridge$getAuthenticatedProfile());
                default -> {
                    return;
                }
            }
        } else {
            return;
        }
        new PlayerConnectionCloseEvent(profile.id(), profile.name(), ((InetSocketAddress) this.address).getAddress(), false).callEvent();
    }
}
