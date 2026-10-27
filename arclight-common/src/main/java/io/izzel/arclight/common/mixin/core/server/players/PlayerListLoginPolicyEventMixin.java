package io.izzel.arclight.common.mixin.core.server.players;

import com.destroystokyo.paper.event.profile.ProfileWhitelistVerifyEvent;
import com.destroystokyo.paper.profile.CraftPlayerProfile;
import io.izzel.arclight.common.bridge.core.server.players.PlayerListLoginPolicyBridge;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.papermc.paper.adventure.PaperAdventure;
import io.papermc.paper.event.player.PlayerServerFullCheckEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.PlayerList;
import net.minecraft.server.players.ServerOpList;
import org.jetbrains.annotations.Nullable;
import org.spigotmc.SpigotConfig;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Slice;

import java.net.SocketAddress;
import java.util.List;

@Mixin(PlayerList.class)
public abstract class PlayerListLoginPolicyEventMixin implements PlayerListLoginPolicyBridge {

    @Shadow @Final private ServerOpList ops;
    @Shadow @Final public List<ServerPlayer> players;
    @Shadow public abstract boolean isUsingWhitelist();
    @Shadow public abstract boolean isWhiteListed(NameAndId profile);
    @Shadow public abstract boolean canBypassPlayerLimit(NameAndId profile);
    @Shadow public abstract int getMaxPlayers();

    @Override
    public @Nullable Component bridge$checkWhitelistLogin(NameAndId profile) {
        return this.arclight$whitelistLoginResult(profile, this.isWhiteListed(profile));
    }

    @Override
    public @Nullable Component bridge$checkFullServerLogin(NameAndId profile) {
        boolean shouldKick = this.players.size() >= this.getMaxPlayers() && !this.canBypassPlayerLimit(profile);
        return this.arclight$fullServerLoginResult(profile, shouldKick, Component.translatable("multiplayer.disconnect.server_full"));
    }

    @Unique
    private @Nullable Component arclight$whitelistLoginResult(NameAndId profile, boolean whitelisted) {
        var configuredMessage = LegacyComponentSerializer.legacySection().deserialize(SpigotConfig.whitelistMessage);
        var event = new ProfileWhitelistVerifyEvent(new CraftPlayerProfile(profile), this.isUsingWhitelist(),
            whitelisted, ((StoredUserListContainsAccessor) this.ops).arclight$contains(profile), configuredMessage);
        event.callEvent();
        return event.isWhitelisted() ? null : PaperAdventure.asVanilla(event.kickMessage() == null ? configuredMessage : event.kickMessage());
    }

    @Unique
    private @Nullable Component arclight$fullServerLoginResult(NameAndId profile, boolean shouldKick, Component message) {
        var event = new PlayerServerFullCheckEvent(new CraftPlayerProfile(profile), PaperAdventure.asAdventure(message), shouldKick);
        event.callEvent();
        return event.isAllowed() ? null : PaperAdventure.asVanilla(event.kickMessage());
    }

    // Only the login callsite dispatches. Other native whitelist queries remain side-effect-free.
    @Decorate(method = "canPlayerLogin(Ljava/net/SocketAddress;Lnet/minecraft/server/players/NameAndId;)Lnet/minecraft/network/chat/Component;",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;isWhiteListed(Lnet/minecraft/server/players/NameAndId;)Z"), require = 1, allow = 1)
    private boolean arclight$verifyWhitelistLogin(PlayerList list, NameAndId profile) throws Throwable {
        boolean whitelisted = (boolean) DecorationOps.callsite().invoke(list, profile);
        Component rejection = this.arclight$whitelistLoginResult(profile, whitelisted);
        if (rejection != null) {
            return (boolean) DecorationOps.cancel().invoke(rejection);
        }
        return true;
    }

    // The slice excludes account-ban, whitelist and IP-ban returns. Vanilla has two
    // full-check returns; NeoForge merges its ternary into one. Keep their native verdict.
    @Decorate(method = "canPlayerLogin(Ljava/net/SocketAddress;Lnet/minecraft/server/players/NameAndId;)Lnet/minecraft/network/chat/Component;",
        at = @At("RETURN"), slice = @Slice(from = @At(value = "FIELD", target = "Lnet/minecraft/server/players/PlayerList;players:Ljava/util/List;")),
        require = 1, allow = 2)
    private void arclight$verifyFullServerLogin(Component result, SocketAddress address, NameAndId profile) throws Throwable {
        result = this.arclight$fullServerLoginResult(profile, result != null,
            result == null ? Component.translatable("multiplayer.disconnect.server_full") : result);
        DecorationOps.callsite().invoke(result);
    }
}
