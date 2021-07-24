package io.izzel.arclight.common.mixin.core.network.handshake.client;

import com.google.gson.Gson;
import com.mojang.authlib.properties.Property;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.handshake.ClientIntentionPacket;
import net.minecraftforge.fml.network.FMLNetworkConstants;
import net.minecraftforge.fml.network.NetworkHooks;
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

    // @formatter:off
    @Shadow(remap = false) private String fmlVersion;
    @Shadow public String hostName;
    // @formatter:on

    private static final String EXTRA_DATA = "extraData";
    private static final Gson GSON = new Gson();

    @Redirect(method = "read", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/FriendlyByteBuf;readUtf(I)Ljava/lang/String;"))
    private String arclight$bungeeHostname(FriendlyByteBuf packetBuffer, int maxLength) {
        return packetBuffer.readUtf(Short.MAX_VALUE);
    }

    @Inject(method = "read", cancellable = true, at = @At(value = "FIELD", shift = At.Shift.AFTER, remap = false, target = "Lnet/minecraft/network/protocol/handshake/ClientIntentionPacket;fmlVersion:Ljava/lang/String;"))
    private void arclight$readFromProfile(FriendlyByteBuf buf, CallbackInfo ci) {
        if (SpigotConfig.bungee && !Objects.equals(this.fmlVersion, FMLNetworkConstants.NETVERSION)) {
            String[] split = this.hostName.split("\0");
            if (split.length == 4) {
                Property[] properties = GSON.fromJson(split[3], Property[].class);
                for (Property property : properties) {
                    if (Objects.equals(property.getName(), EXTRA_DATA)) {
                        String extraData = property.getValue().replace("\1", "\0");
                        this.fmlVersion = NetworkHooks.getFMLVersion(split[0] + extraData);
                    }
                }
            }
        }
        ci.cancel();
    }
}
