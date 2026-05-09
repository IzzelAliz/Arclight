package io.izzel.arclight.common.mixin.core.server.dedicated;

import io.izzel.arclight.common.bridge.bukkit.CraftServerBridge;
import net.minecraft.server.dedicated.DedicatedPlayerList;
import org.bukkit.Bukkit;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/*
 * Fix adventure-platform-fabric replacing List<ServerPlayer> causing
 * Bukkit#getOnlinePlayers returns nothing
 */
@Mixin(DedicatedPlayerList.class)
public class DedicatedPlayerListMixin {
    @Inject(method = "<init>", at = @At(value = "TAIL"))
    private void arclight$afterSuper(CallbackInfo ci) {
        ((CraftServerBridge)Bukkit.getServer()).bridge$setPlayerList((DedicatedPlayerList)(Object) this);
    }
}
