package io.izzel.arclight.common.mixin.core.server.players;

import com.destroystokyo.paper.profile.CraftPlayerProfile;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import io.papermc.paper.event.server.WhitelistStateUpdateEvent;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.UserWhiteList;
import net.minecraft.server.players.UserWhiteListEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(UserWhiteList.class)
public abstract class UserWhiteListStateEventMixin {

    @Decorate(method = "add(Lnet/minecraft/server/players/UserWhiteListEntry;)Z", inject = true, at = @At("HEAD"), require = 1, allow = 1)
    private void arclight$whitelistAdded(UserWhiteListEntry entry) throws Throwable {
        var event = new WhitelistStateUpdateEvent(new CraftPlayerProfile(entry.getUser()),
            WhitelistStateUpdateEvent.WhitelistStatus.ADDED);
        if (!event.callEvent()) {
            DecorationOps.cancel().invoke(false);
            return;
        }
    }

    @Decorate(method = "remove(Lnet/minecraft/server/players/NameAndId;)Z", inject = true, at = @At("HEAD"), require = 1, allow = 1)
    private void arclight$whitelistRemoved(NameAndId profile) throws Throwable {
        var event = new WhitelistStateUpdateEvent(new CraftPlayerProfile(profile),
            WhitelistStateUpdateEvent.WhitelistStatus.REMOVED);
        if (!event.callEvent()) {
            DecorationOps.cancel().invoke(false);
            return;
        }
    }
}
