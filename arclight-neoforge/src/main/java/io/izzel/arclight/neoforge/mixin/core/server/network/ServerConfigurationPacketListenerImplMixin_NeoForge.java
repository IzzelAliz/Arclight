package io.izzel.arclight.neoforge.mixin.core.server.network;

import io.izzel.arclight.common.mod.server.ArclightServer;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerConfigurationPacketListenerImpl.class)
public abstract class ServerConfigurationPacketListenerImplMixin_NeoForge extends ServerCommonPacketListenerImplMixin_NeoForge {

    // @formatter:off
    @Shadow protected abstract void startConfiguration();
    @Shadow private void runConfiguration() { throw new AssertionError(); }
    @Shadow public abstract void finishCurrentTask(net.minecraft.server.network.ConfigurationTask.Type type);

    @Inject(method = "finishCurrentTask", cancellable = true, at = @At("HEAD"))
    private void arclight$finishTaskOnMainThread(net.minecraft.server.network.ConfigurationTask.Type type, CallbackInfo ci) {
        // NeoForge custom-payload acknowledgements may arrive on a Netty thread.
        // Completing one task can immediately start PrepareSpawnTask and mutate chunk tickets.
        if (!ArclightServer.isPrimaryThread()) {
            ArclightServer.executeOnMainThread(() -> this.finishCurrentTask(type));
            ci.cancel();
        }
    }
    // @formatter:on

    @Inject(method = "runConfiguration", cancellable = true, at = @At("HEAD"))
    private void arclight$runDeferredConfigurationMainThread(CallbackInfo ci) {
        if (!ArclightServer.isPrimaryThread()) {
            ArclightServer.executeOnMainThread(this::runConfiguration);
            ci.cancel();
        }
    }

    @Inject(method = "startConfiguration", cancellable = true, at = @At("HEAD"))
    private void arclight$runConfigurationMainThread(CallbackInfo ci) {
        if (!ArclightServer.isPrimaryThread()) {
            ArclightServer.executeOnMainThread(this::startConfiguration);
            ci.cancel();
        }
    }
}
