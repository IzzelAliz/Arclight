package io.izzel.arclight.common.mixin.core.network.play.client;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import net.minecraft.network.play.IServerPlayNetHandler;
import net.minecraft.network.play.client.CChatMessagePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Mixin(CChatMessagePacket.class)
public class CChatMessagePacketMixin {

    @Shadow private String message;

    private static final ExecutorService executors = Executors.newCachedThreadPool(
        new ThreadFactoryBuilder().setDaemon(true).setNameFormat("Async Chat Thread - #%d").build()
    );

    @Inject(method = "processPacket", cancellable = true, at = @At("HEAD"))
    private void arclight$asyncChat(IServerPlayNetHandler handler, CallbackInfo ci) {
        if (!this.message.startsWith("/")) {
            executors.submit(() -> handler.processChatMessage((CChatMessagePacket) (Object) this));
            ci.cancel();
        }
    }
}
