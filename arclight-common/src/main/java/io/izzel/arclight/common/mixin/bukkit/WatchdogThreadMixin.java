package io.izzel.arclight.common.mixin.bukkit;

import io.izzel.arclight.common.bridge.core.server.MinecraftServerBridge;
import io.izzel.arclight.common.mod.server.ArclightServer;
import net.minecraft.util.Util;
import org.spigotmc.WatchdogThread;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.locks.LockSupport;

@Mixin(value = WatchdogThread.class, remap = false)
public class WatchdogThreadMixin extends Thread {

    @Shadow private static WatchdogThread instance;

    @Inject(method = "tick", at = @At("RETURN"))
    private static void arclight$tick(CallbackInfo ci) {
        ((MinecraftServerBridge) ArclightServer.getMinecraftServer()).arclight$extendNextTickTimeTo(Util.timeSource);
    }

    @Inject(method = "doStop", at = @At("HEAD"))
    private static void arclight$doStop(CallbackInfo ci) {
        if (instance != null) instance.interrupt();
    }

    @Inject(method = "<init>", at = @At(value = "RETURN"))
    private void arclight$setDaemon(long timeoutTime, boolean restart, CallbackInfo ci) {
        setDaemon(true);
    }

    /**
     * @author InitAuther97
     * @reason Actually running Mojang watchdog, no need to run.
     */
    @Overwrite
    public void run() {
        ArclightServer.LOGGER.info("Started pseudo Spigot watchdog thread.");
        ArclightServer.LOGGER.debug("Spigot watchdog thread run() stack trace", new UnsupportedOperationException("started spigot watchdog"));
        while (!Thread.interrupted()) {
            LockSupport.parkNanos(Long.MAX_VALUE);
        }
    }

}
