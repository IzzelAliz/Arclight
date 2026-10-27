package io.izzel.arclight.common.mixin.core.server;

import io.papermc.paper.connection.PaperConfigurationTask;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = MinecraftServer.class, priority = 1100)
public abstract class MinecraftServerConfigurationTaskMixin {

    @Inject(method = "stopServer()V", at = @At("HEAD"), require = 1, expect = 1, allow = 1)
    private void arclight$shutdownConfigurationPool(CallbackInfo ci) {
        // Run before plugin teardown and even when startup failed before CraftServer existed.
        // Neither plugin disable nor resource reload owns this server-global executor.
        PaperConfigurationTask.shutdownConfigurationPool();
    }
}
