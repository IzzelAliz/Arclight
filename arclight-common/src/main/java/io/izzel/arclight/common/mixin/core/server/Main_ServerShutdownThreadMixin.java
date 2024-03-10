package io.izzel.arclight.common.mixin.core.server;

import net.minecraft.server.MinecraftServer;
import org.spigotmc.AsyncCatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "net/minecraft/server/Main$1")
public class Main_ServerShutdownThreadMixin {

    @Redirect(method = "run", require = 0, at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;halt(Z)V"))
    private void arclight$shutdown(MinecraftServer instance, boolean b) {
        AsyncCatcher.enabled = false;
        instance.close();
    }
}
