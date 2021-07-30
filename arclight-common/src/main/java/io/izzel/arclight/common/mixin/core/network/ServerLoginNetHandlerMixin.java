package io.izzel.arclight.common.mixin.core.network;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import io.izzel.arclight.common.bridge.core.network.NetworkManagerBridge;
import io.izzel.arclight.common.bridge.core.network.login.ServerLoginNetHandlerBridge;
import io.izzel.arclight.common.bridge.core.server.MinecraftServerBridge;
import io.izzel.arclight.common.bridge.core.server.management.PlayerListBridge;
import net.minecraft.DefaultUncaughtExceptionHandler;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.protocol.game.ClientboundDisconnectPacket;
import net.minecraft.network.protocol.login.ClientboundGameProfilePacket;
import net.minecraft.network.protocol.login.ClientboundHelloPacket;
import net.minecraft.network.protocol.login.ClientboundLoginCompressionPacket;
import net.minecraft.network.protocol.login.ClientboundLoginDisconnectPacket;
import net.minecraft.network.protocol.login.ServerboundHelloPacket;
import net.minecraft.network.protocol.login.ServerboundKeyPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import net.minecraft.util.Crypt;
import net.minecraft.util.CryptException;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.util.thread.SidedThreadGroups;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.Logger;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v.CraftServer;
import org.bukkit.craftbukkit.v.util.Waitable;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerPreLoginEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.Nullable;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.PrivateKey;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Mixin(ServerLoginPacketListenerImpl.class)
public abstract class ServerLoginNetHandlerMixin implements ServerLoginNetHandlerBridge {

    // @formatter:off
    @Shadow private ServerLoginPacketListenerImpl.State state;
    @Shadow @Final private MinecraftServer server;
    @Shadow @Final private byte[] nonce;
    @Shadow @Final public Connection connection;
    @Shadow @Final private static AtomicInteger UNIQUE_THREAD_ID;
    @Shadow private GameProfile gameProfile;
    @Shadow @Final private static Logger LOGGER;
    @Shadow protected abstract GameProfile createFakeProfile(GameProfile original);
    @Shadow public abstract void disconnect(Component reason);
    @Shadow public abstract String getUserName();
    @Shadow private ServerPlayer delayedAcceptPlayer;
    // @formatter:on

    public String hostname;

    @Override
    public String bridge$getHostname() {
        return hostname;
    }

    @Override
    public void bridge$setHostname(String hostname) {
        this.hostname = hostname;
    }

    public void disconnect(final String s) {
        try {
            final Component ichatbasecomponent = new TextComponent(s);
            LOGGER.info("Disconnecting {}: {}", this.getUserName(), s);
            this.connection.send(new ClientboundLoginDisconnectPacket(ichatbasecomponent));
            this.connection.disconnect(ichatbasecomponent);
        } catch (Exception exception) {
            LOGGER.error("Error whilst disconnecting player", exception);
        }
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public void handleAcceptedLogin() {
        /*
        if (!this.loginGameProfile.isComplete()) {
            this.loginGameProfile = this.getOfflineProfile(this.loginGameProfile);
        }
        */

        ServerPlayer entity = ((PlayerListBridge) this.server.getPlayerList()).bridge$canPlayerLogin(this.connection.getRemoteAddress(), this.gameProfile, (ServerLoginPacketListenerImpl) (Object) this);
        if (entity == null) {
            // this.disconnect(itextcomponent);
        } else {
            this.state = ServerLoginPacketListenerImpl.State.ACCEPTED;
            if (this.server.getCompressionThreshold() >= 0 && !this.connection.isMemoryConnection()) {
                this.connection.send(new ClientboundLoginCompressionPacket(this.server.getCompressionThreshold()), (p_210149_1_) -> {
                    this.connection.setupCompression(this.server.getCompressionThreshold(), true);
                });
            }

            this.connection.send(new ClientboundGameProfilePacket(this.gameProfile));
            ServerPlayer serverplayerentity = this.server.getPlayerList().getPlayer(this.gameProfile.getId());
            try {
                if (serverplayerentity != null) {
                    this.state = ServerLoginPacketListenerImpl.State.DELAY_ACCEPT;
                    this.delayedAcceptPlayer = entity;
                } else {
                    this.server.getPlayerList().placeNewPlayer(this.connection, entity);
                }
            } catch (Exception exception) {
                TranslatableComponent chatmessage = new TranslatableComponent("multiplayer.disconnect.invalid_player_data");

                this.connection.send(new ClientboundDisconnectPacket(chatmessage));
                this.connection.disconnect(chatmessage);
            }
        }
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public void handleHello(ServerboundHelloPacket packetIn) {
        Validate.validState(this.state == ServerLoginPacketListenerImpl.State.HELLO, "Unexpected hello packet");
        this.gameProfile = packetIn.getGameProfile();
        if (this.server.usesAuthentication() && !this.connection.isMemoryConnection()) {
            this.state = ServerLoginPacketListenerImpl.State.KEY;
            this.connection.send(new ClientboundHelloPacket("", this.server.getKeyPair().getPublic().getEncoded(), this.nonce));
        } else {
            class Handler extends Thread {

                Handler() {
                    super(SidedThreadGroups.SERVER, "User Authenticator #" + UNIQUE_THREAD_ID.incrementAndGet());
                }

                @Override
                public void run() {
                    try {
                        initUUID();
                        arclight$preLogin();
                    } catch (Exception ex) {
                        disconnect("Failed to verify username!");
                        LOGGER.warn("Exception verifying {} ", gameProfile.getName(), ex);
                    }
                }
            }
            new Handler().start();
        }

    }

    public void initUUID() {
        UUID uuid;
        if (((NetworkManagerBridge) this.connection).bridge$getSpoofedUUID() != null) {
            uuid = ((NetworkManagerBridge) this.connection).bridge$getSpoofedUUID();
        } else {
            uuid = Player.createPlayerUUID(this.gameProfile.getName());
        }
        this.gameProfile = new GameProfile(uuid, this.gameProfile.getName());
        if (((NetworkManagerBridge) this.connection).bridge$getSpoofedProfile() != null) {
            Property[] spoofedProfile;
            for (int length = (spoofedProfile = ((NetworkManagerBridge) this.connection).bridge$getSpoofedProfile()).length, i = 0; i < length; ++i) {
                final Property property = spoofedProfile[i];
                this.gameProfile.getProperties().put(property.getName(), property);
            }
        }
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public void handleKey(ServerboundKeyPacket packetIn) {
        Validate.validState(this.state == ServerLoginPacketListenerImpl.State.KEY, "Unexpected key packet");
        PrivateKey privatekey = this.server.getKeyPair().getPrivate();

        final String s;
        try {
            if (!Arrays.equals(this.nonce, packetIn.getNonce(privatekey))) {
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

        class Handler extends Thread {

            Handler(int i) {
                super(SidedThreadGroups.SERVER, "User Authenticator #" + i);
            }

            public void run() {
                GameProfile gameprofile = gameProfile;

                try {
                    gameProfile = server.getSessionService().hasJoinedServer(new GameProfile(null, gameprofile.getName()), s, this.getAddress());
                    if (gameProfile != null) {
                        if (!connection.isConnected()) {
                            return;
                        }
                        arclight$preLogin();
                    } else if (server.isSingleplayer()) {
                        LOGGER.warn("Failed to verify username but will let them in anyway!");
                        gameProfile = createFakeProfile(gameprofile);
                        state = ServerLoginPacketListenerImpl.State.NEGOTIATING;
                    } else {
                        disconnect(new TranslatableComponent("multiplayer.disconnect.unverified_username"));
                        LOGGER.error("Username '{}' tried to join with an invalid session", gameprofile.getName());
                    }
                } catch (Exception var3) {
                    if (server.isSingleplayer()) {
                        LOGGER.warn("Authentication servers are down but will let them in anyway!");
                        gameProfile = createFakeProfile(gameprofile);
                        state = ServerLoginPacketListenerImpl.State.NEGOTIATING;
                    } else {
                        disconnect(new TranslatableComponent("multiplayer.disconnect.authservers_down"));
                        LOGGER.error("Couldn't verify username because servers are unavailable");
                    }
                }

            }

            @Nullable
            private InetAddress getAddress() {
                SocketAddress socketaddress = connection.getRemoteAddress();
                return server.getPreventProxyConnections() && socketaddress instanceof InetSocketAddress ? ((InetSocketAddress) socketaddress).getAddress() : null;
            }
        }
        Thread thread = new Handler(UNIQUE_THREAD_ID.incrementAndGet());
        thread.setUncaughtExceptionHandler(new DefaultUncaughtExceptionHandler(LOGGER));
        thread.start();
    }

    void arclight$preLogin() throws Exception {
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
        state = ServerLoginPacketListenerImpl.State.NEGOTIATING;
    }
}
