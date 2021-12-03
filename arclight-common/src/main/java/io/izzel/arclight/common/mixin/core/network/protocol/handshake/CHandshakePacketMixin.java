package io.izzel.arclight.common.mixin.core.network.protocol.handshake;

import com.google.gson.Gson;
import com.mojang.authlib.properties.Property;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.handshake.ClientIntentionPacket;
import net.minecraftforge.network.NetworkConstants;
import net.minecraftforge.network.NetworkHooks;
import org.spigotmc.SpigotConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

@Mixin(ClientIntentionPacket.class)
public class CHandshakePacketMixin {

    private static final String EXTRA_DATA = "extraData";
    private static final Gson GSON = new Gson();

    @Shadow public String hostName;

    @Redirect(method = "<init>(Lnet/minecraft/network/FriendlyByteBuf;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/FriendlyByteBuf;readUtf(I)Ljava/lang/String;"))
    private String arclight$bungeeHostname(FriendlyByteBuf packetBuffer, int maxLength) {
        return packetBuffer.readUtf(Short.MAX_VALUE);
    }

    @Redirect(method = "<init>(Lnet/minecraft/network/FriendlyByteBuf;)V", at = @At(value = "INVOKE", remap = false, target = "Lnet/minecraftforge/network/NetworkHooks;getFMLVersion(Ljava/lang/String;)Ljava/lang/String;"))
    private String arclight$readFromProfile(String ip) {
        String fmlVersion = NetworkHooks.getFMLVersion(ip);
        if (SpigotConfig.bungee && !Objects.equals(fmlVersion, NetworkConstants.NETVERSION)) {
            String[] split = ip.split("\0");
            if (split.length == 4) {
                Property[] properties = GSON.fromJson(split[3], Property[].class);
                for (Property property : properties) {
                    if (Objects.equals(property.getName(), EXTRA_DATA)) {
                        String extraData = property.getValue().replace("\1", "\0");
                        this.arclight$host = ip;
                        return NetworkHooks.getFMLVersion(split[0] + extraData);
                    }
                }
            }
        }
        return fmlVersion;
    }

    private transient String arclight$host;

    @Inject(method = "<init>(Lnet/minecraft/network/FriendlyByteBuf;)V", at = @At("RETURN"))
    private void arclight$writeBack(FriendlyByteBuf p_179801_, CallbackInfo ci) {
        if (arclight$host != null) {
            this.hostName = arclight$host;
            arclight$host = null;
        }
    }
}
