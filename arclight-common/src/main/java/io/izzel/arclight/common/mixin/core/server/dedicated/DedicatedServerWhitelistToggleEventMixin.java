package io.izzel.arclight.common.mixin.core.server.dedicated;

import com.destroystokyo.paper.event.server.WhitelistToggleEvent;
import io.izzel.arclight.mixin.Decorate;
import net.minecraft.server.dedicated.DedicatedServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(DedicatedServer.class)
public abstract class DedicatedServerWhitelistToggleEventMixin {

    @Decorate(method = "setUsingWhitelist(Z)V", inject = true, at = @At("HEAD"), require = 1, allow = 1)
    private void arclight$whitelistToggled(boolean enabled) {
        new WhitelistToggleEvent(enabled).callEvent();
    }
}
