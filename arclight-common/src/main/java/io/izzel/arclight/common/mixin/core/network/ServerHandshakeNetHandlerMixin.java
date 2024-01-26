package io.izzel.arclight.common.mixin.core.network;

import com.google.gson.Gson;
import com.mojang.authlib.properties.Property;
import com.mojang.util.UndashedUuid;
import io.izzel.arclight.common.bridge.core.network.NetworkManagerBridge;
import io.izzel.arclight.common.bridge.core.network.handshake.ServerHandshakeNetHandlerBridge;
import io.izzel.arclight.common.mod.util.VelocitySupport;
import net.minecraft.SharedConstants;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.handshake.ClientIntent;
import net.minecraft.network.protocol.handshake.ClientIntentionPacket;
import net.minecraft.network.protocol.login.ClientboundLoginDisconnectPacket;
import net.minecraft.network.protocol.status.ServerStatus;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerHandshakePacketListenerImpl;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import net.minecraft.server.network.ServerStatusPacketListenerImpl;
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
public abstract class ServerHandshakeNetHandlerMixin implements ServerHandshakeNetHandlerBridge {

    private static final Gson gson = new Gson();
    private static final java.util.regex.Pattern HOST_PATTERN = java.util.regex.Pattern.compile("[0-9a-f\\.:]{0,45}");
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
        if (!bridge$forge$handleSpecialLogin(packetIn)) {
            return;
        }
        ((NetworkManagerBridge) this.connection).bridge$setHostname(packetIn.hostName() + ":" + packetIn.port());
        switch (packetIn.intention()) {
            case LOGIN: {
                this.connection.setClientboundProtocolAfterHandshake(ClientIntent.LOGIN);

                try {
                    long currentTime = System.currentTimeMillis();
                    long connectionThrottle = Bukkit.getServer().getConnectionThrottle();
                    InetAddress address = ((InetSocketAddress) this.connection.getRemoteAddress()).getAddress();
                    synchronized (throttleTracker) {
                        if (throttleTracker.containsKey(address) && !"127.0.0.1".equals(address.getHostAddress()) && currentTime - throttleTracker.get(address) < connectionThrottle) {
                            throttleTracker.put(address, currentTime);
                            var component = Component.literal("Connection throttled! Please wait before reconnecting.");
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


                if (packetIn.protocolVersion() > SharedConstants.getCurrentVersion().getProtocolVersion()) {
                    var component = Component.translatable(MessageFormat.format(SpigotConfig.outdatedServerMessage.replaceAll("'", "''"), SharedConstants.getCurrentVersion().getName()));
                    this.connection.send(new ClientboundLoginDisconnectPacket(component));
                    this.connection.disconnect(component);
                    break;
                }
                if (packetIn.protocolVersion() < SharedConstants.getCurrentVersion().getProtocolVersion()) {
                    var component = Component.translatable(MessageFormat.format(SpigotConfig.outdatedClientMessage.replaceAll("'", "''"), SharedConstants.getCurrentVersion().getName()));
                    this.connection.send(new ClientboundLoginDisconnectPacket(component));
                    this.connection.disconnect(component);
                    break;
                }
                this.connection.setListener(new ServerLoginPacketListenerImpl(this.server, this.connection));

                if (!VelocitySupport.isEnabled()) {
                    String[] split = packetIn.hostName().split("\00");
                    if (SpigotConfig.bungee) {
                        if ((split.length == 3 || split.length == 4) && (HOST_PATTERN.matcher(split[1]).matches())) {
                            ((NetworkManagerBridge) this.connection).bridge$setHostname(split[0]);
                            this.connection.address = new InetSocketAddress(split[1], ((InetSocketAddress) this.connection.getRemoteAddress()).getPort());
                            ((NetworkManagerBridge) this.connection).bridge$setSpoofedUUID(UndashedUuid.fromStringLenient(split[2]));
                        } else {
                            var component = Component.literal("If you wish to use IP forwarding, please enable it in your BungeeCord config as well!");
                            this.connection.send(new ClientboundLoginDisconnectPacket(component));
                            this.connection.disconnect(component);
                            return;
                        }
                        if (split.length == 4) {
                            ((NetworkManagerBridge) this.connection).bridge$setSpoofedProfile(gson.fromJson(split[3], Property[].class));
                        }
                    } else if ((split.length == 3 || split.length == 4) && (HOST_PATTERN.matcher(split[1]).matches())) {
                        Component component = Component.literal("Unknown data in login hostname, did you forget to enable BungeeCord in spigot.yml?");
                        this.connection.send(new ClientboundLoginDisconnectPacket(component));
                        this.connection.disconnect(component);
                        return;
                    }
                }

                break;
            }
            case STATUS: {
                ServerStatus serverstatus = this.server.getStatus();
                if (this.server.repliesToStatus() && serverstatus != null) {
                    this.connection.setClientboundProtocolAfterHandshake(ClientIntent.STATUS);
                    this.connection.setListener(new ServerStatusPacketListenerImpl(serverstatus, this.connection));
                } else {
                    this.connection.disconnect(IGNORE_STATUS_REASON);
                }
                break;
            }
            default: {
                throw new UnsupportedOperationException("Invalid intention " + packetIn.intention());
            }
        }
    }
}
