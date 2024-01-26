package io.izzel.arclight.forge.mixin.core.network;

import com.google.gson.Gson;
import com.mojang.authlib.properties.Property;
import io.izzel.arclight.common.bridge.core.network.handshake.ServerHandshakeNetHandlerBridge;
import io.izzel.arclight.common.mod.util.VelocitySupport;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.handshake.ClientIntentionPacket;
import net.minecraft.server.network.ServerHandshakePacketListenerImpl;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.spigotmc.SpigotConfig;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Objects;

@Mixin(ServerHandshakePacketListenerImpl.class)
public abstract class ServerHandshakeNetHandlerMixin_Forge implements ServerHandshakeNetHandlerBridge {

    // @formatter:off
    @Shadow @Final private Connection connection;
    // @formatter:on

    private static final String EXTRA_DATA = "extraData";
    private static final Gson GSON = new Gson();

    @Override
    public boolean bridge$forge$handleSpecialLogin(ClientIntentionPacket packet) {
        String ip = packet.hostName();
        if (!VelocitySupport.isEnabled() && SpigotConfig.bungee) {
            String[] split = ip.split("\0");
            if (split.length == 4) {
                Property[] properties = GSON.fromJson(split[3], Property[].class);
                for (Property property : properties) {
                    if (Objects.equals(property.name(), EXTRA_DATA)) {
                        String extraData = property.value().replace("\1", "\0");
                        // replace the hostname field with embedded data
                        //noinspection deprecation
                        var forgePacket = new ClientIntentionPacket(packet.protocolVersion(), extraData, packet.port(), packet.intention());
                        return ServerLifecycleHooks.handleServerLogin(forgePacket, this.connection);
                    }
                }
            }
        }
        return ServerLifecycleHooks.handleServerLogin(packet, this.connection);
    }
}
