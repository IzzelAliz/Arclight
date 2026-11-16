package io.izzel.arclight.common.mixin.core.server;

import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.server.dedicated.DedicatedServer;
import org.spigotmc.AsyncCatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "net/minecraft/server/Main$1")
public class Main_ServerShutdownThreadMixin {

    @Decorate(method = "run", require = 0, at = @At(value = "INVOKE", target = "Lnet/minecraft/server/dedicated/DedicatedServer;halt(Z)V"))
    private void arclight$shutdown(DedicatedServer instance, boolean b) throws Throwable {
        AsyncCatcher.enabled = false;
        DecorationOps.callsite().invoke(instance, instance.isRunning() && b);
    }
}