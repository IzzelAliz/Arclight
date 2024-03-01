package io.izzel.arclight.common.mixin.core.network;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.properties.Property;
import io.izzel.arclight.common.bridge.core.network.NetworkManagerBridge;
import io.izzel.arclight.common.bridge.core.network.common.ServerCommonPacketListenerBridge;
import io.izzel.arclight.common.bridge.core.network.login.ServerLoginNetHandlerBridge;
import io.izzel.arclight.common.bridge.core.server.MinecraftServerBridge;
import io.izzel.arclight.common.bridge.core.server.management.PlayerListBridge;
import io.izzel.arclight.common.mod.util.VelocitySupport;
import net.minecraft.DefaultUncaughtExceptionHandler;
import net.minecraft.Util;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.login.ClientboundCustomQueryPacket;
import net.minecraft.network.protocol.login.ClientboundHelloPacket;
import net.minecraft.network.protocol.login.ServerboundCustomQueryAnswerPacket;
import net.minecraft.network.protocol.login.ServerboundHelloPacket;
import net.minecraft.network.protocol.login.ServerboundKeyPacket;
import net.minecraft.network.protocol.login.ServerboundLoginAcknowledgedPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import net.minecraft.server.players.PlayerList;
import net.minecraft.util.Crypt;
import net.minecraft.util.CryptException;
import net.minecraft.world.entity.player.Player;
import org.apache.commons.lang3.Validate;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.CraftServer;
import org.bukkit.craftbukkit.v.util.Waitable;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerPreLoginEvent;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import javax.annotation.Nullable;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.PrivateKey;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

@Mixin(ServerLoginPacketListenerImpl.class)
public abstract class ServerLoginNetHandlerMixin implements ServerLoginNetHandlerBridge {

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
    @Shadow protected abstract boolean isPlayerAlreadyInWorld(GameProfile p_298499_);
    @Shadow @Nullable private GameProfile authenticatedProfile;
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

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public void handleHello(ServerboundHelloPacket packetIn) {
        Validate.validState(this.state == ServerLoginPacketListenerImpl.State.HELLO, "Unexpected hello packet");
        Validate.validState(Player.isValidUsername(packetIn.name()), "Invalid characters in username");
        this.requestedUsername = packetIn.name();
        GameProfile gameprofile = this.server.getSingleplayerProfile();
        if (gameprofile != null && this.requestedUsername.equalsIgnoreCase(gameprofile.getName())) {
            this.startClientVerification(gameprofile);
        } else {
            if (this.server.usesAuthentication() && !this.connection.isMemoryConnection()) {
                this.state = ServerLoginPacketListenerImpl.State.KEY;
                this.connection.send(new ClientboundHelloPacket("", this.server.getKeyPair().getPublic().getEncoded(), this.challenge));
            } else {
                if (VelocitySupport.isEnabled()) {
                    this.arclight$velocityLoginId = ThreadLocalRandom.current().nextInt();
                    var packet = new ClientboundCustomQueryPacket(this.arclight$velocityLoginId, VelocitySupport.createPacket());
                    this.connection.send(packet);
                    return;
                }

                var thread = bridge$newHandleThread("User Authenticator #" + UNIQUE_THREAD_ID.incrementAndGet(), () -> {
                    try {
                        var gameProfile = arclight$createOfflineProfile(connection, requestedUsername);
                        bridge$preLogin(gameProfile);
                    } catch (Exception ex) {
                        disconnect(Component.translatable("multiplayer.disconnect.unverified_username"));
                        LOGGER.warn("Exception verifying {} ", requestedUsername, ex);
                    }
                });
                thread.start();
            }
        }
    }

    private static GameProfile arclight$createOfflineProfile(Connection connection, String name) {
        UUID uuid;
        if (((NetworkManagerBridge) connection).bridge$getSpoofedUUID() != null) {
            uuid = ((NetworkManagerBridge) connection).bridge$getSpoofedUUID();
        } else {
            uuid = UUIDUtil.createOfflinePlayerUUID(name);
        }

        GameProfile gameProfile = new GameProfile(uuid, name);

        if (((NetworkManagerBridge) connection).bridge$getSpoofedProfile() != null) {
            Property[] spoofedProfile;
            for (int length = (spoofedProfile = ((NetworkManagerBridge) connection).bridge$getSpoofedProfile()).length, i = 0; i < length; ++i) {
                final Property property = spoofedProfile[i];
                if (!PROP_PATTERN.matcher(property.name()).matches()) continue;
                gameProfile.getProperties().put(property.name(), property);
            }
        }
        return gameProfile;
    }

    @Redirect(method = "verifyLoginAndFinishConnectionSetup", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;canPlayerLogin(Ljava/net/SocketAddress;Lcom/mojang/authlib/GameProfile;)Lnet/minecraft/network/chat/Component;"))
    private Component arclight$canLogin(PlayerList instance, SocketAddress socketAddress, GameProfile gameProfile) {
        this.player = ((PlayerListBridge) instance).bridge$canPlayerLogin(socketAddress, gameProfile, (ServerLoginPacketListenerImpl) (Object) this);
        return null;
    }

    @Inject(method = "verifyLoginAndFinishConnectionSetup", cancellable = true, at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/server/players/PlayerList;canPlayerLogin(Ljava/net/SocketAddress;Lcom/mojang/authlib/GameProfile;)Lnet/minecraft/network/chat/Component;"))
    private void arclight$returnIfFail(GameProfile p_299507_, CallbackInfo ci) {
        if (this.player == null) {
            ci.cancel();
        }
    }

    @Redirect(method = "verifyLoginAndFinishConnectionSetup", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;disconnectAllPlayersWithProfile(Lcom/mojang/authlib/GameProfile;)Z"))
    private boolean arclight$skipKick(PlayerList instance, GameProfile gameProfile) {
        return this.isPlayerAlreadyInWorld(Objects.requireNonNull(this.authenticatedProfile));
    }

    @Inject(method = "handleLoginAcknowledgement", locals = LocalCapture.CAPTURE_FAILHARD, at = @At(value = "INVOKE", target = "Lnet/minecraft/network/Connection;setListener(Lnet/minecraft/network/PacketListener;)V"))
    private void arclight$setPlayer(ServerboundLoginAcknowledgedPacket p_298815_, CallbackInfo ci, CommonListenerCookie cookie, ServerConfigurationPacketListenerImpl listener) {
        ((ServerCommonPacketListenerBridge) listener).bridge$setPlayer(this.player);
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public void handleKey(ServerboundKeyPacket packetIn) {
        Validate.validState(this.state == ServerLoginPacketListenerImpl.State.KEY, "Unexpected key packet");

        final String s;
        try {
            PrivateKey privatekey = this.server.getKeyPair().getPrivate();
            if (!packetIn.isChallengeValid(this.challenge, privatekey)) {
                throw new IllegalStateException("Protocol error");
            }

            SecretKey secretKey = packetIn.getSecretKey(privatekey);
            Cipher cipher = Crypt.getCipher(2, secretKey);
            Cipher cipher1 = Crypt.getCipher(1, secretKey);
            s = (new BigInteger(Crypt.digestData("", this.server.getKeyPair().getPublic(), secretKey))).toString(16);
            this.state = ServerLoginPacketListenerImpl.State.AUTHENTICATING;
            this.connection.setEncryptionKey(cipher, cipher1);
        } catch (CryptException cryptexception) {
            throw new IllegalStateException("Protocol error", cryptexception);
        }

        var thread = bridge$newHandleThread("User Authenticator #" + UNIQUE_THREAD_ID.incrementAndGet(), () -> {
            String name = Objects.requireNonNull(requestedUsername, "Player name not initialized");

            try {
                SocketAddress socketaddress = connection.getRemoteAddress();
                var address = server.getPreventProxyConnections() && socketaddress instanceof InetSocketAddress ? ((InetSocketAddress) socketaddress).getAddress() : null;

                var profileResult = server.getSessionService().hasJoinedServer(name, s, address);
                if (profileResult != null) {
                    var gameProfile = profileResult.profile();
                    if (!connection.isConnected()) {
                        return;
                    }
                    bridge$preLogin(gameProfile);
                } else if (server.isSingleplayer()) {
                    LOGGER.warn("Failed to verify username but will let them in anyway!");
                    startClientVerification(arclight$createOfflineProfile(connection, name));
                } else {
                    disconnect(Component.translatable("multiplayer.disconnect.unverified_username"));
                    LOGGER.error("Username '{}' tried to join with an invalid session", name);
                }
            } catch (AuthenticationException e) {
                if (server.isSingleplayer()) {
                    LOGGER.warn("Authentication servers are down but will let them in anyway!");
                    startClientVerification(arclight$createOfflineProfile(connection, name));
                } else {
                    disconnect(Component.translatable("multiplayer.disconnect.authservers_down"));
                    LOGGER.error("Couldn't verify username because servers are unavailable");
                }
            } catch (Exception e) {
                disconnect(Component.translatable("multiplayer.disconnect.unverified_username"));
                LOGGER.error("Exception verifying " + name, e);
            }
        });
        thread.setUncaughtExceptionHandler(new DefaultUncaughtExceptionHandler(LOGGER));
        thread.start();
    }

    @Unique
    public void bridge$preLogin(GameProfile gameProfile) throws Exception {
        if (this.arclight$velocityLoginId == -1 && VelocitySupport.isEnabled()) {
            disconnect("This server requires you to connect with Velocity.");
            return;
        }
        String playerName = gameProfile.getName();
        InetAddress address = ((InetSocketAddress) connection.getRemoteAddress()).getAddress();
        UUID uniqueId = gameProfile.getId();
        CraftServer craftServer = (CraftServer) Bukkit.getServer();
        AsyncPlayerPreLoginEvent asyncEvent = new AsyncPlayerPreLoginEvent(playerName, address, uniqueId);
        craftServer.getPluginManager().callEvent(asyncEvent);
        if (PlayerPreLoginEvent.getHandlerList().getRegisteredListeners().length != 0) {
            PlayerPreLoginEvent event = new PlayerPreLoginEvent(playerName, address, uniqueId);
            if (asyncEvent.getResult() != PlayerPreLoginEvent.Result.ALLOWED) {
                event.disallow(asyncEvent.getResult(), asyncEvent.getKickMessage());
            }
            class SyncPreLogin extends Waitable<PlayerPreLoginEvent.Result> {

                @Override
                protected PlayerPreLoginEvent.Result evaluate() {
                    craftServer.getPluginManager().callEvent(event);
                    return event.getResult();
                }
            }
            Waitable<PlayerPreLoginEvent.Result> waitable = new SyncPreLogin();
            ((MinecraftServerBridge) server).bridge$queuedProcess(waitable);
            if (waitable.get() != PlayerPreLoginEvent.Result.ALLOWED) {
                disconnect(event.getKickMessage());
                return;
            }
        } else if (asyncEvent.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) {
            disconnect(asyncEvent.getKickMessage());
            return;
        }
        LOGGER.info("UUID of player {} is {}", gameProfile.getName(), gameProfile.getId());
        this.startClientVerification(gameProfile);
    }

    @Inject(method = "handleCustomQueryPacket", cancellable = true, at = @At("HEAD"))
    private void arclight$modernForwardReply(ServerboundCustomQueryAnswerPacket packet, CallbackInfo ci) {
        if (VelocitySupport.isEnabled() && packet.transactionId() == this.bridge$getVelocityLoginId()) {
            var payload = bridge$getDiscardedQueryAnswerData(packet);
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
                    this.bridge$preLogin(this.authenticatedProfile);
                } catch (Exception ex) {
                    disconnect(Component.translatable("multiplayer.disconnect.unverified_username"));
                    LOGGER.warn("Exception verifying {} ", this.authenticatedProfile.getName(), ex);
                }
            });
            this.bridge$platform$onCustomQuery(packet);
            ci.cancel();
        }
    }
}
