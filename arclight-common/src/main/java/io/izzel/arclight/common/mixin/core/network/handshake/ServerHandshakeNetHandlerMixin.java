package io.izzel.arclight.common.mixin.core.network.handshake;

import com.google.gson.Gson;
import com.mojang.authlib.properties.Property;
import com.mojang.util.UUIDTypeAdapter;
import io.izzel.arclight.common.bridge.network.NetworkManagerBridge;
import io.izzel.arclight.common.bridge.network.login.ServerLoginNetHandlerBridge;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.ProtocolType;
import net.minecraft.network.handshake.ServerHandshakeNetHandler;
import net.minecraft.network.handshake.client.CHandshakePacket;
import net.minecraft.network.login.ServerLoginNetHandler;
import net.minecraft.network.login.server.SDisconnectLoginPacket;
import net.minecraft.network.status.ServerStatusNetHandler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.SharedConstants;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
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

@Mixin(ServerHandshakeNetHandler.class)
public class ServerHandshakeNetHandlerMixin {

    private static final Gson gson = new Gson();
    private static final HashMap<InetAddress, Long> throttleTracker = new HashMap<>();
    private static int throttleCounter = 0;

    // @formatter:off
    @Shadow @Final private NetworkManager networkManager;
    @Shadow @Final private MinecraftServer server;
    @Shadow @Final private static ITextComponent field_241169_a_;
    // @formatter:on

    /**
     * @author IzzelAliz
     * @reason
     */
    @Overwrite
    public void processHandshake(CHandshakePacket packetIn) {
        if (!ServerLifecycleHooks.handleServerLogin(packetIn, this.networkManager)) return;
        switch (packetIn.getRequestedState()) {
            case LOGIN: {
                this.networkManager.setConnectionState(ProtocolType.LOGIN);

                // todo forge use ip field for storing forge data, this may be an issue
                try {
                    long currentTime = System.currentTimeMillis();
                    long connectionThrottle = Bukkit.getServer().getConnectionThrottle();
                    InetAddress address = ((InetSocketAddress) this.networkManager.getRemoteAddress()).getAddress();
                    synchronized (throttleTracker) {
                        if (throttleTracker.containsKey(address) && !"127.0.0.1".equals(address.getHostAddress()) && currentTime - throttleTracker.get(address) < connectionThrottle) {
                            throttleTracker.put(address, currentTime);
                            TranslationTextComponent component = new TranslationTextComponent("Connection throttled! Please wait before reconnecting.");
                            this.networkManager.sendPacket(new SDisconnectLoginPacket(component));
                            this.networkManager.closeChannel(component);
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


                if (packetIn.getProtocolVersion() > SharedConstants.getVersion().getProtocolVersion()) {
                    TranslationTextComponent component = new TranslationTextComponent(MessageFormat.format(SpigotConfig.outdatedServerMessage.replaceAll("'", "''"), SharedConstants.getVersion().getName()));
                    this.networkManager.sendPacket(new SDisconnectLoginPacket(component));
                    this.networkManager.closeChannel(component);
                    break;
                }
                if (packetIn.getProtocolVersion() < SharedConstants.getVersion().getProtocolVersion()) {
                    TranslationTextComponent component = new TranslationTextComponent(MessageFormat.format(SpigotConfig.outdatedClientMessage.replaceAll("'", "''"), SharedConstants.getVersion().getName()));
                    this.networkManager.sendPacket(new SDisconnectLoginPacket(component));
                    this.networkManager.closeChannel(component);
                    break;
                }
                this.networkManager.setNetHandler(new ServerLoginNetHandler(this.server, this.networkManager));


                if (SpigotConfig.bungee) {
                    String[] split = packetIn.ip.split("\00");
                    if (split.length == 3 || split.length == 4) {
                        packetIn.ip = split[0];
                        this.networkManager.socketAddress = new InetSocketAddress(split[1], ((InetSocketAddress) this.networkManager.getRemoteAddress()).getPort());
                        ((NetworkManagerBridge) this.networkManager).bridge$setSpoofedUUID(UUIDTypeAdapter.fromString(split[2]));
                    } else {
                        TranslationTextComponent component = new TranslationTextComponent("If you wish to use IP forwarding, please enable it in your BungeeCord config as well!");
                        this.networkManager.sendPacket(new SDisconnectLoginPacket(component));
                        this.networkManager.closeChannel(component);
                        return;
                    }
                    if (split.length == 4) {
                        ((NetworkManagerBridge) this.networkManager).bridge$setSpoofedProfile(gson.fromJson(split[3], Property[].class));
                    }
                }
                ((ServerLoginNetHandlerBridge) this.networkManager.getNetHandler()).bridge$setHostname(packetIn.ip + ":" + packetIn.port);


                break;
            }
            case STATUS: {
                if (this.server.func_230541_aj_()) {
                    this.networkManager.setConnectionState(ProtocolType.STATUS);
                    this.networkManager.setNetHandler(new ServerStatusNetHandler(this.server, this.networkManager));
                } else {
                    this.networkManager.closeChannel(field_241169_a_);
                }
                break;
            }
            default: {
                throw new UnsupportedOperationException("Invalid intention " + packetIn.getRequestedState());
            }
        }
    }
}
