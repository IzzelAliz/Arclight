package io.izzel.arclight.mixin.core.network.login;

import com.mojang.authlib.GameProfile;
import io.izzel.arclight.bridge.server.MinecraftServerBridge;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.login.ServerLoginNetHandler;
import net.minecraft.network.login.client.CEncryptionResponsePacket;
import net.minecraft.network.login.client.CLoginStartPacket;
import net.minecraft.network.login.server.SDisconnectLoginPacket;
import net.minecraft.network.login.server.SEncryptionRequestPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.CryptManager;
import net.minecraft.util.DefaultUncaughtExceptionHandler;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.fml.common.thread.SidedThreadGroups;
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
import javax.crypto.SecretKey;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.PrivateKey;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Mixin(ServerLoginNetHandler.class)
public abstract class ServerLoginNetHandler1Mixin {

    // @formatter:off
    @Shadow private ServerLoginNetHandler.State currentLoginState;
    @Shadow @Final private MinecraftServer server;
    @Shadow @Final private byte[] verifyToken;
    @Shadow private SecretKey secretKey;
    @Shadow @Final public NetworkManager networkManager;
    @Shadow @Final private static AtomicInteger AUTHENTICATOR_THREAD_ID;
    @Shadow private GameProfile loginGameProfile;
    @Shadow @Final private static Logger LOGGER;
    @Shadow protected abstract GameProfile getOfflineProfile(GameProfile original);
    @Shadow public abstract void disconnect(ITextComponent reason);
    @Shadow public abstract String getConnectionInfo();
    // @formatter:on

    public void disconnect(final String s) {
        try {
            final ITextComponent ichatbasecomponent = new StringTextComponent(s);
            LOGGER.info("Disconnecting {}: {}", this.getConnectionInfo(), s);
            this.networkManager.sendPacket(new SDisconnectLoginPacket(ichatbasecomponent));
            this.networkManager.closeChannel(ichatbasecomponent);
        } catch (Exception exception) {
            LOGGER.error("Error whilst disconnecting player", exception);
        }
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public void processLoginStart(CLoginStartPacket packetIn) {
        Validate.validState(this.currentLoginState == ServerLoginNetHandler.State.HELLO, "Unexpected hello packet");
        this.loginGameProfile = packetIn.getProfile();
        if (this.server.isServerInOnlineMode() && !this.networkManager.isLocalChannel()) {
            this.currentLoginState = ServerLoginNetHandler.State.KEY;
            this.networkManager.sendPacket(new SEncryptionRequestPacket("", this.server.getKeyPair().getPublic(), this.verifyToken));
        } else {
            class Handler extends Thread {

                Handler() {
                    super(SidedThreadGroups.SERVER, "User Authenticator #" + AUTHENTICATOR_THREAD_ID.incrementAndGet());
                }

                @Override
                public void run() {
                    try {
                        initUUID();
                        arclight$preLogin();
                    } catch (Exception ex) {
                        disconnect("Failed to verify username!");
                        LOGGER.warn("Exception verifying {} ", loginGameProfile.getName(), ex);
                    }
                }
            }
            new Handler().start();
        }

    }

    public void initUUID() {
        UUID uuid = PlayerEntity.getOfflineUUID(this.loginGameProfile.getName());
        this.loginGameProfile = new GameProfile(uuid, this.loginGameProfile.getName());
    }

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public void processEncryptionResponse(CEncryptionResponsePacket packetIn) {
        Validate.validState(this.currentLoginState == ServerLoginNetHandler.State.KEY, "Unexpected key packet");
        PrivateKey privatekey = this.server.getKeyPair().getPrivate();
        if (!Arrays.equals(this.verifyToken, packetIn.getVerifyToken(privatekey))) {
            throw new IllegalStateException("Invalid nonce!");
        } else {
            this.secretKey = packetIn.getSecretKey(privatekey);
            this.currentLoginState = ServerLoginNetHandler.State.AUTHENTICATING;
            this.networkManager.enableEncryption(this.secretKey);
            class Handler extends Thread {

                Handler() {
                    super(SidedThreadGroups.SERVER, "User Authenticator #" + AUTHENTICATOR_THREAD_ID.incrementAndGet());
                }

                public void run() {
                    GameProfile gameprofile = loginGameProfile;

                    try {
                        String s = (new BigInteger(CryptManager.getServerIdHash("", server.getKeyPair().getPublic(), secretKey))).toString(16);
                        loginGameProfile = server.getMinecraftSessionService().hasJoinedServer(new GameProfile((UUID) null, gameprofile.getName()), s, this.getAddress());
                        if (loginGameProfile != null) {
                            if (!networkManager.isChannelOpen()) {
                                return;
                            }
                            arclight$preLogin();
                        } else if (server.isSinglePlayer()) {
                            LOGGER.warn("Failed to verify username but will let them in anyway!");
                            loginGameProfile = getOfflineProfile(gameprofile);
                            currentLoginState = ServerLoginNetHandler.State.NEGOTIATING;
                        } else {
                            disconnect(new TranslationTextComponent("multiplayer.disconnect.unverified_username"));
                            LOGGER.error("Username '{}' tried to join with an invalid session", (Object) gameprofile.getName());
                        }
                    } catch (Exception var3) {
                        if (server.isSinglePlayer()) {
                            LOGGER.warn("Authentication servers are down but will let them in anyway!");
                            loginGameProfile = getOfflineProfile(gameprofile);
                            currentLoginState = ServerLoginNetHandler.State.NEGOTIATING;
                        } else {
                            disconnect(new TranslationTextComponent("multiplayer.disconnect.authservers_down"));
                            LOGGER.error("Couldn't verify username because servers are unavailable");
                        }
                    }

                }

                @Nullable
                private InetAddress getAddress() {
                    SocketAddress socketaddress = networkManager.getRemoteAddress();
                    return server.getPreventProxyConnections() && socketaddress instanceof InetSocketAddress ? ((InetSocketAddress) socketaddress).getAddress() : null;
                }
            }
            Thread thread = new Handler();
            thread.setUncaughtExceptionHandler(new DefaultUncaughtExceptionHandler(LOGGER));
            thread.start();
        }
    }

    private void arclight$preLogin() throws Exception {
        String playerName = loginGameProfile.getName();
        InetAddress address = ((InetSocketAddress) networkManager.getRemoteAddress()).getAddress();
        UUID uniqueId = loginGameProfile.getId();
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
        LOGGER.info("UUID of player {} is {}", loginGameProfile.getName(), loginGameProfile.getId());
        currentLoginState = ServerLoginNetHandler.State.READY_TO_ACCEPT;
    }
}
