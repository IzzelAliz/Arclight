package io.izzel.arclight.common.mixin.core.server.network;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import io.izzel.arclight.common.bridge.core.network.ConnectionBridge;
import io.izzel.arclight.common.bridge.core.server.network.ServerCommonPacketListenerImplBridge;
import io.izzel.arclight.common.bridge.core.server.network.ServerLoginPacketListenerImplBridge;
import io.izzel.arclight.common.bridge.core.server.level.ServerPlayerBridge;
import io.izzel.arclight.common.bridge.core.server.players.PlayerListBridge;
import io.izzel.arclight.common.mod.util.VelocitySupport;
import net.minecraft.DefaultUncaughtExceptionHandler;
import net.minecraft.util.Util;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.Connection;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketUtils;
import net.minecraft.network.protocol.cookie.ServerboundCookieResponsePacket;
import net.minecraft.network.protocol.login.ClientboundCustomQueryPacket;
import net.minecraft.network.protocol.login.ServerboundCustomQueryAnswerPacket;
import net.minecraft.network.protocol.login.ServerboundHelloPacket;
import net.minecraft.network.protocol.login.ServerboundLoginAcknowledgedPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import net.minecraft.server.players.PlayerList;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import javax.annotation.Nullable;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

@Mixin(ServerLoginPacketListenerImpl.class)
public abstract class ServerLoginPacketListenerImplMixin implements ServerLoginPacketListenerImplBridge, CraftPlayer.TransferCookieConnection {

    // @formatter:off
    @Shadow private ServerLoginPacketListenerImpl.State state;
    @Shadow @Final private MinecraftServer server;
    @Shadow @Final public Connection connection;
    @Shadow @Final private static AtomicInteger UNIQUE_THREAD_ID;
    @Shadow @Final private static Logger LOGGER;
    @Shadow public abstract void disconnect(Component reason);
    @Shadow public abstract String getUserName();
    @Shadow @Final private byte[] challenge;
    @Shadow @Nullable private String requestedUsername;
    @Shadow abstract void startClientVerification(GameProfile p_301095_);
    @Invoker("callPlayerPreLoginEvents")
    protected abstract void arclight$callPlayerPreLoginEvents(GameProfile profile) throws Exception;
    @Shadow protected abstract boolean isPlayerAlreadyInWorld(GameProfile p_298499_);
    @Shadow @Nullable private GameProfile authenticatedProfile;
    @Shadow @Final private boolean transferred;
    // @formatter:on

    private static final java.util.regex.Pattern PROP_PATTERN = java.util.regex.Pattern.compile("\\w{0,16}");

    private ServerPlayer player;
    @Unique protected int arclight$velocityLoginId = -1;

    @Override
    public int bridge$getVelocityLoginId() {
        return arclight$velocityLoginId;
    }

    @Override
    public void bridge$disconnect(String s) {
        this.disconnect(Component.literal(s));
    }

    public void disconnect(final String s) {
        bridge$disconnect(s);
    }

    @Inject(method = "handleHello", cancellable = true, at = @At(value = "NEW", target = "java/lang/Thread", shift = At.Shift.BEFORE))
    private void arclight$velocityHello(ServerboundHelloPacket packet, CallbackInfo ci) {
        if ((!this.server.usesAuthentication() || this.connection.isMemoryConnection()) && VelocitySupport.isEnabled()) {
            this.arclight$velocityLoginId = ThreadLocalRandom.current().nextInt();
            this.connection.send(new ClientboundCustomQueryPacket(this.arclight$velocityLoginId, VelocitySupport.createPacket()));
            ci.cancel();
        }
    }

    private static GameProfile arclight$createOfflineProfile(Connection connection, String name) {
        UUID uuid;
        if (((ConnectionBridge) connection).bridge$getSpoofedUUID() != null) {
            uuid = ((ConnectionBridge) connection).bridge$getSpoofedUUID();
        } else {
            uuid = UUIDUtil.createOfflinePlayerUUID(name);
        }

        GameProfile gameProfile = new GameProfile(uuid, name);

        if (((ConnectionBridge) connection).bridge$getSpoofedProfile() != null) {
            Property[] spoofedProfile;
            for (int length = (spoofedProfile = ((ConnectionBridge) connection).bridge$getSpoofedProfile()).length, i = 0; i < length; ++i) {
                final Property property = spoofedProfile[i];
                if (!PROP_PATTERN.matcher(property.name()).matches()) continue;
                gameProfile.properties().put(property.name(), property);
            }
        }
        return gameProfile;
    }

    @Redirect(method = "verifyLoginAndFinishConnectionSetup", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;canPlayerLogin(Lnet/minecraft/server/network/ServerLoginPacketListenerImpl;Lcom/mojang/authlib/GameProfile;)Lnet/minecraft/server/level/ServerPlayer;"))
    private ServerPlayer arclight$canLogin(PlayerList instance, ServerLoginPacketListenerImpl listener, GameProfile gameProfile) {
        SocketAddress socketAddress = this.connection.getRemoteAddress();
        return ((PlayerListBridge) instance).bridge$canPlayerLogin(socketAddress, gameProfile, listener);
    }

    @Redirect(method = "postCookies", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;disconnectAllPlayersWithProfile(Ljava/util/UUID;Lnet/minecraft/server/level/ServerPlayer;)Z"))
    private boolean arclight$skipKick(PlayerList instance, UUID uuid, ServerPlayer serverPlayer) {
        return this.isPlayerAlreadyInWorld(Objects.requireNonNull(this.authenticatedProfile));
    }

    @Inject(method = "handleLoginAcknowledgement", locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", target = "Lnet/minecraft/network/Connection;setupInboundProtocol(Lnet/minecraft/network/ProtocolInfo;Lnet/minecraft/network/PacketListener;)V"))
    private void arclight$setPlayer(ServerboundLoginAcknowledgedPacket p_298815_, CallbackInfo ci, CommonListenerCookie cookie, ServerConfigurationPacketListenerImpl listener) {
        ((ServerCommonPacketListenerImplBridge) listener).bridge$setPlayer(this.player);
    }

    /*

     * Forgified Fabric API (FFAPI) will actively record every custom query and awaits all responses
     * before we enter the configuration stage. Due to their powerful control on queries we must allow
     * them to at least have a glance on what they receive.
     * FFAPI selected its injection point at the HEAD of this method. Thus, we selected INVOKE disconnect
     * to ensure a defined injection order.
     * Due to lack of support on custom queries in Forge/NF, FFAPI aggressively deserialize all CustomQA
     * payload into its own kind; it is thus needed to take special care when processing the payload.
     * Fallback implementation will log a loud warning and try to serialize the custom payload to recreate
     * original answer data. This does not work for FFAPI since their payload is a buffer wrapper and has
     * consumed the buffer by the end of their handler.
     * See Forge/NF CustomQA deserialization & ArclightCustomQueryAnswerPayload.
     * See FFAPI compat impl for customQAData & onCustomQA.
     */
    @Inject(method = "handleCustomQueryPacket", cancellable = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/server/network/ServerLoginPacketListenerImpl;disconnect(Lnet/minecraft/network/chat/Component;)V"))
    private void arclight$modernForwardReply(ServerboundCustomQueryAnswerPacket packet, CallbackInfo ci) {
        if (VelocitySupport.isEnabled() && packet.transactionId() == this.bridge$getVelocityLoginId()) {
            var payload = arclight$platform$customQAData(packet);
            if (payload == null) {
                this.bridge$disconnect("This server requires you to connect with Velocity.");
                ci.cancel();
                return;
            }
            var buf = payload.readNullable(r -> {
                int i = r.readableBytes();
                if (i >= 0 && i <= 1048576) {
                    return new FriendlyByteBuf(r.readBytes(i));
                } else {
                    throw new IllegalArgumentException("Payload may not be larger than 1048576 bytes");
                }
            });
            if (buf == null) {
                this.bridge$disconnect("This server requires you to connect with Velocity.");
                ci.cancel();
                return;
            }

            if (!VelocitySupport.checkIntegrity(buf)) {
                this.bridge$disconnect("Unable to verify player details");
                ci.cancel();
                return;
            }

            int version = buf.readVarInt();
            if (version > VelocitySupport.MAX_SUPPORTED_FORWARDING_VERSION) {
                throw new IllegalStateException("Unsupported forwarding version " + version + ", wanted upto " + VelocitySupport.MAX_SUPPORTED_FORWARDING_VERSION);
            }
            java.net.SocketAddress listening = this.connection.getRemoteAddress();
            int port = 0;
            if (listening instanceof java.net.InetSocketAddress) {
                port = ((java.net.InetSocketAddress) listening).getPort();
            }
            this.connection.address = new java.net.InetSocketAddress(VelocitySupport.readAddress(buf), port);
            this.authenticatedProfile = VelocitySupport.createProfile(buf);

            // Proceed with login
            Util.backgroundExecutor().execute(() -> {
                try {
                    if (this.arclight$velocityLoginId == -1 && VelocitySupport.isEnabled()) {
                        disconnect("This server requires you to connect with Velocity.");
                        return;
                    }
                    this.arclight$callPlayerPreLoginEvents(this.authenticatedProfile);
                    LOGGER.info("UUID of player {} is {}", this.authenticatedProfile.name(), this.authenticatedProfile.id());
                    this.startClientVerification(this.authenticatedProfile);
                } catch (Exception ex) {
                    disconnect(Component.translatable("multiplayer.disconnect.unverified_username"));
                    LOGGER.warn("Exception verifying {} ", this.authenticatedProfile.name(), ex);
                }
            });
            this.arclight$platform$onCustomQA(packet);
            ci.cancel();
        }
    }

}
