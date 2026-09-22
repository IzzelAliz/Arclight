package io.izzel.arclight.common.mixin.core.server.rcon;

import io.izzel.arclight.common.bridge.core.server.dedicated.DedicatedServerBridge;
import net.minecraft.server.ServerInterface;
import net.minecraft.server.rcon.thread.RconClient;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.net.Socket;

@Mixin(RconClient.class)
public class RconClientMixin {
    @Shadow @Final private Socket client;

    @Redirect(method = "run()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/ServerInterface;runCommand(Ljava/lang/String;)Ljava/lang/String;"))
    private String arclight$runCommandWithAddress(ServerInterface server, String command) {
        if (server instanceof DedicatedServerBridge bridge) {
            return bridge.bridge$runRconCommand(command, this.client.getRemoteSocketAddress());
        }
        return server.runCommand(command);
    }
}
