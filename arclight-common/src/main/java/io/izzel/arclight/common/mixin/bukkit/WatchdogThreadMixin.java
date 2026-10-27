package io.izzel.arclight.common.mixin.bukkit;

import io.izzel.arclight.common.bridge.core.server.MinecraftServerBridge;
import io.izzel.arclight.common.mod.server.ArclightServer;
import net.minecraft.util.Util;
import org.spigotmc.WatchdogThread;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = WatchdogThread.class, remap = false)
public class WatchdogThreadMixin {

    @Inject(method = "tick", at = @At("RETURN"))
    private static void arclight$tick(CallbackInfo ci) {
        ((MinecraftServerBridge) ArclightServer.getMinecraftServer()).arclight$extendNextTickTimeTo(Util.timeSource);
    }
}
