package io.izzel.arclight.common.mod.util;

import com.google.common.net.InetAddresses;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import io.izzel.arclight.i18n.ArclightConfig;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.login.custom.CustomQueryPayload;
import net.minecraft.network.protocol.login.custom.DiscardedQueryPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.ProfilePublicKey;
import org.jetbrains.annotations.NotNull;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.InetAddress;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

// Codes from paper, GPLv3/MIT
// https://github.com/PaperMC/Paper/blob/master/patches/server/0832-Add-Velocity-IP-Forwarding-Support.patch
// Changes are made to use Arclight configuration system
public class VelocitySupport {

    public static boolean isEnabled() {
        return ArclightConfig.spec().getVelocity().isEnable();
    }

    private static final int SUPPORTED_FORWARDING_VERSION = 1;
    public static final int MODERN_FORWARDING_WITH_KEY = 2;
    public static final int MODERN_FORWARDING_WITH_KEY_V2 = 3;
    public static final int MODERN_LAZY_SESSION = 4;
    public static final byte MAX_SUPPORTED_FORWARDING_VERSION = MODERN_LAZY_SESSION;
    public static final ResourceLocation PLAYER_INFO_CHANNEL = new ResourceLocation("velocity", "player_info");

    private record VelocityForwardQuery(FriendlyByteBuf data) implements CustomQueryPayload {

        @Override
        public @NotNull ResourceLocation id() {
            return PLAYER_INFO_CHANNEL;
        }

        @Override
        public void write(@NotNull FriendlyByteBuf buf) {
            buf.writeBytes(this.data.slice());
        }
    }

    public static CustomQueryPayload createPacket() {
        var buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeByte(VelocitySupport.MAX_SUPPORTED_FORWARDING_VERSION);
        return new VelocityForwardQuery(buf);
    }

    public static boolean checkIntegrity(final FriendlyByteBuf buf) {
        final byte[] signature = new byte[32];
        buf.readBytes(signature);

        final byte[] data = new byte[buf.readableBytes()];
        buf.getBytes(buf.readerIndex(), data);

        try {
            final Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(ArclightConfig.spec().getVelocity().getSecret().getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256"));
            final byte[] mySignature = mac.doFinal(data);
            if (!MessageDigest.isEqual(signature, mySignature)) {
                return false;
            }
        } catch (final InvalidKeyException | NoSuchAlgorithmException e) {
            throw new AssertionError(e);
        }

        return true;
    }

    public static InetAddress readAddress(final FriendlyByteBuf buf) {
        return InetAddresses.forString(buf.readUtf(Short.MAX_VALUE));
    }

    public static GameProfile createProfile(final FriendlyByteBuf buf) {
        final GameProfile profile = new GameProfile(buf.readUUID(), buf.readUtf(16));
        readProperties(buf, profile);
        return profile;
    }

    private static void readProperties(final FriendlyByteBuf buf, final GameProfile profile) {
        final int properties = buf.readVarInt();
        for (int i1 = 0; i1 < properties; i1++) {
            final String name = buf.readUtf(Short.MAX_VALUE);
            final String value = buf.readUtf(Short.MAX_VALUE);
            final String signature = buf.readBoolean() ? buf.readUtf(Short.MAX_VALUE) : null;
            profile.getProperties().put(name, new Property(name, value, signature));
        }
    }

    public static ProfilePublicKey.Data readForwardedKey(FriendlyByteBuf buf) {
        return new ProfilePublicKey.Data(buf);
    }

    public static UUID readSignerUuidOrElse(FriendlyByteBuf buf, UUID orElse) {
        return buf.readBoolean() ? buf.readUUID() : orElse;
    }
}
