package io.izzel.arclight.common.mixin.core.server.rcon;

import io.izzel.arclight.common.bridge.core.server.dedicated.DedicatedServerBridge;
import net.minecraft.server.ServerInterface;
import net.minecraft.server.rcon.thread.RconClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.net.Socket;

@Mixin(RconClient.class)
public class RconClientMixin {
    @org.spongepowered.asm.mixin.Unique
    private net.minecraft.server.rcon.RconConsoleSource arclight$source;

    @org.spongepowered.asm.mixin.injection.Inject(method = "<init>(Lnet/minecraft/server/ServerInterface;Ljava/lang/String;Ljava/net/Socket;)V", at = @At("RETURN"))
    private void arclight$createSource(ServerInterface server, String password, Socket socket, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        if (server instanceof net.minecraft.server.MinecraftServer minecraftServer && server instanceof DedicatedServerBridge) {
            this.arclight$source = new net.minecraft.server.rcon.RconConsoleSource(minecraftServer);
            ((io.izzel.arclight.common.bridge.core.server.rcon.RconConsoleSourceBridge) this.arclight$source).bridge$setSocketAddress(socket.getRemoteSocketAddress());
        }
    }

    @Redirect(method = "run()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/ServerInterface;runCommand(Ljava/lang/String;)Ljava/lang/String;"))
    private String arclight$runCommandWithAddress(ServerInterface server, String command) {
        if (server instanceof DedicatedServerBridge bridge && this.arclight$source != null) {
            return bridge.bridge$runRconCommand(this.arclight$source, command);
        }
        return server.runCommand(command);
    }
}
