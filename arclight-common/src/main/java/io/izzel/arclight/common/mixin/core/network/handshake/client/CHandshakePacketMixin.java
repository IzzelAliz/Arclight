package io.izzel.arclight.common.mixin.core.network.handshake.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.handshake.client.CHandshakePacket;
import net.minecraftforge.fml.network.FMLNetworkConstants;
import net.minecraftforge.fml.network.NetworkHooks;
import org.spigotmc.SpigotConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.Objects;

@Mixin(CHandshakePacket.class)
public class CHandshakePacketMixin {

    // @formatter:off
    @Shadow(remap = false) private String fmlVersion;
    @Shadow public String ip;
    @Shadow private int protocolVersion;
    // @formatter:on

    private static final String EXTRA_DATA = "extraData";
    private static final Gson GSON = new GsonBuilder().registerTypeAdapter(PropertyMap.class, new PropertyMap.Serializer()).create();

    @Redirect(method = "readPacketData", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/PacketBuffer;readString(I)Ljava/lang/String;"))
    private String arclight$bungeeHostname(PacketBuffer packetBuffer, int maxLength) {
        return packetBuffer.readString(Short.MAX_VALUE);
    }

    @Inject(method = "readPacketData", cancellable = true, at = @At(value = "FIELD", shift = At.Shift.AFTER, remap = false, target = "Lnet/minecraft/network/handshake/client/CHandshakePacket;fmlVersion:Ljava/lang/String;"))
    private void arclight$readFromProfile(PacketBuffer buf, CallbackInfo ci) {
        if (SpigotConfig.bungee && !Objects.equals(this.fmlVersion, FMLNetworkConstants.NETVERSION)) {
            String[] split = this.ip.split("\0");
            if (split.length == 4) {
                PropertyMap properties = GSON.fromJson(split[3], PropertyMap.class);
                for (Map.Entry<String, Property> property : properties.entries()) {
                    if (property.getKey().equalsIgnoreCase("forgeClient")) {
                        if (property.getValue().getValue().equalsIgnoreCase(FMLNetworkConstants.NETVERSION)) {
                            this.fmlVersion = FMLNetworkConstants.NETVERSION;
                        }
                    }

                    if (Objects.equals(property.getValue().getName(), EXTRA_DATA)) {
                        String extraData = property.getValue().getValue().replace("\1", "\0");
                        this.fmlVersion = NetworkHooks.getFMLVersion(split[0] + extraData);
                    }
                }
            }
        }
        ci.cancel();
    }
}