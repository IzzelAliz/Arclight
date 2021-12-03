package io.izzel.arclight.common.mixin.core.network;

import com.google.gson.Gson;
import com.mojang.authlib.properties.Property;
import com.mojang.util.UUIDTypeAdapter;
import io.izzel.arclight.common.bridge.core.network.NetworkManagerBridge;
import io.izzel.arclight.common.bridge.core.network.login.ServerLoginNetHandlerBridge;
import net.minecraft.SharedConstants;
import net.minecraft.network.Connection;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.protocol.handshake.ClientIntentionPacket;
import net.minecraft.network.protocol.login.ClientboundLoginDisconnectPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerHandshakePacketListenerImpl;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import net.minecraft.server.network.ServerStatusPacketListenerImpl;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.apache.logging.log4j.LogManager;
import org.bukkit.Bukkit;
import org.spigotmc.SpigotConfig;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.text.MessageFormat;
import java.util.HashMap;

@Mixin(ServerHandshakePacketListenerImpl.class)
public class ServerHandshakeNetHandlerMixin {

    private static final Gson gson = new Gson();
    private static final HashMap<InetAddress, Long> throttleTracker = new HashMap<>();
    private static int throttleCounter = 0;

    // @formatter:off
    @Shadow @Final private Connection connection;
    @Shadow @Final private MinecraftServer server;
    @Shadow @Final private static Component IGNORE_STATUS_REASON;
    // @formatter:on

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public void handleIntention(ClientIntentionPacket packetIn) {
        if (!ServerLifecycleHooks.handleServerLogin(packetIn, this.connection)) return;
        switch (packetIn.getIntention()) {
            case LOGIN: {
                this.connection.setProtocol(ConnectionProtocol.LOGIN);

                try {
                    long currentTime = System.currentTimeMillis();
                    long connectionThrottle = Bukkit.getServer().getConnectionThrottle();
                    InetAddress address = ((InetSocketAddress) this.connection.getRemoteAddress()).getAddress();
                    synchronized (throttleTracker) {
                        if (throttleTracker.containsKey(address) && !"127.0.0.1".equals(address.getHostAddress()) && currentTime - throttleTracker.get(address) < connectionThrottle) {
                            throttleTracker.put(address, currentTime);
                            TranslatableComponent component = new TranslatableComponent("Connection throttled! Please wait before reconnecting.");
                            this.connection.send(new ClientboundLoginDisconnectPacket(component));
                            this.connection.disconnect(component);
                            return;
                        }
                        throttleTracker.put(address, currentTime);
                        ++throttleCounter;
                        if (throttleCounter > 200) {
                            throttleCounter = 0;
                            throttleTracker.entrySet().removeIf(entry -> entry.getValue() > connectionThrottle);
                        }
                    }
                } catch (Throwable t) {
                    LogManager.getLogger().debug("Failed to check connection throttle", t);
                }


                if (packetIn.getProtocolVersion() > SharedConstants.getCurrentVersion().getProtocolVersion()) {
                    TranslatableComponent component = new TranslatableComponent(MessageFormat.format(SpigotConfig.outdatedServerMessage.replaceAll("'", "''"), SharedConstants.getCurrentVersion().getName()));
                    this.connection.send(new ClientboundLoginDisconnectPacket(component));
                    this.connection.disconnect(component);
                    break;
                }
                if (packetIn.getProtocolVersion() < SharedConstants.getCurrentVersion().getProtocolVersion()) {
                    TranslatableComponent component = new TranslatableComponent(MessageFormat.format(SpigotConfig.outdatedClientMessage.replaceAll("'", "''"), SharedConstants.getCurrentVersion().getName()));
                    this.connection.send(new ClientboundLoginDisconnectPacket(component));
                    this.connection.disconnect(component);
                    break;
                }
                this.connection.setListener(new ServerLoginPacketListenerImpl(this.server, this.connection));


                if (SpigotConfig.bungee) {
                    String[] split = packetIn.hostName.split("\00");
                    if (split.length == 3 || split.length == 4) {
                        packetIn.hostName = split[0];
                        this.connection.address = new InetSocketAddress(split[1], ((InetSocketAddress) this.connection.getRemoteAddress()).getPort());
                        ((NetworkManagerBridge) this.connection).bridge$setSpoofedUUID(UUIDTypeAdapter.fromString(split[2]));
                    } else {
                        TranslatableComponent component = new TranslatableComponent("If you wish to use IP forwarding, please enable it in your BungeeCord config as well!");
                        this.connection.send(new ClientboundLoginDisconnectPacket(component));
                        this.connection.disconnect(component);
                        return;
                    }
                    if (split.length == 4) {
                        ((NetworkManagerBridge) this.connection).bridge$setSpoofedProfile(gson.fromJson(split[3], Property[].class));
                    }
                }
                ((ServerLoginNetHandlerBridge) this.connection.getPacketListener()).bridge$setHostname(packetIn.hostName + ":" + packetIn.port);


                break;
            }
            case STATUS: {
                if (this.server.repliesToStatus()) {
                    this.connection.setProtocol(ConnectionProtocol.STATUS);
                    this.connection.setListener(new ServerStatusPacketListenerImpl(this.server, this.connection));
                } else {
                    this.connection.disconnect(IGNORE_STATUS_REASON);
                }
                break;
            }
            default: {
                throw new UnsupportedOperationException("Invalid intention " + packetIn.getIntention());
            }
        }
    }
}
