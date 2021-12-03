package io.izzel.arclight.common.mixin.core.network.protocol.game;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import net.minecraft.network.protocol.game.ServerGamePacketListener;
import net.minecraft.network.protocol.game.ServerboundChatPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Mixin(ServerboundChatPacket.class)
public class CChatMessagePacketMixin {

    @Shadow @Final private String message;

    private static final ExecutorService executors = Executors.newCachedThreadPool(
        new ThreadFactoryBuilder().setDaemon(true).setNameFormat("Async Chat Thread - #%d").build()
    );

    @Inject(method = "handle(Lnet/minecraft/network/protocol/game/ServerGamePacketListener;)V", cancellable = true, at = @At("HEAD"))
    private void arclight$asyncChat(ServerGamePacketListener handler, CallbackInfo ci) {
        if (!this.message.startsWith("/")) {
            executors.submit(() -> handler.handleChat((ServerboundChatPacket) (Object) this));
            ci.cancel();
        }
    }
}
