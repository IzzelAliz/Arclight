package io.izzel.arclight.common.mixin.core.network.protocol.handshake;

import com.google.gson.Gson;
import com.mojang.authlib.properties.Property;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.handshake.ClientIntentionPacket;
import net.minecraftforge.fmllegacy.network.FMLNetworkConstants;
import net.minecraftforge.fmllegacy.network.NetworkHooks;
import org.spigotmc.SpigotConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Objects;

@Mixin(ClientIntentionPacket.class)
public class CHandshakePacketMixin {

    private static final String EXTRA_DATA = "extraData";
    private static final Gson GSON = new Gson();

    @Redirect(method = "<init>(Lnet/minecraft/network/FriendlyByteBuf;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/FriendlyByteBuf;readUtf(I)Ljava/lang/String;"))
    private String arclight$bungeeHostname(FriendlyByteBuf packetBuffer, int maxLength) {
        return packetBuffer.readUtf(Short.MAX_VALUE);
    }

    @Redirect(method = "<init>(Lnet/minecraft/network/FriendlyByteBuf;)V", at = @At(value = "INVOKE", remap = false, target = "Lnet/minecraftforge/fmllegacy/network/NetworkHooks;getFMLVersion(Ljava/lang/String;)Ljava/lang/String;"))
    private String arclight$readFromProfile(String ip) {
        String fmlVersion = NetworkHooks.getFMLVersion(ip);
        if (SpigotConfig.bungee && !Objects.equals(fmlVersion, FMLNetworkConstants.NETVERSION)) {
            String[] split = ip.split("\0");
            if (split.length == 4) {
                Property[] properties = GSON.fromJson(split[3], Property[].class);
                for (Property property : properties) {
                    if (Objects.equals(property.getName(), EXTRA_DATA)) {
                        String extraData = property.getValue().replace("\1", "\0");
                        return NetworkHooks.getFMLVersion(split[0] + extraData);
                    }
                }
            }
        }
        return fmlVersion;
    }
}
