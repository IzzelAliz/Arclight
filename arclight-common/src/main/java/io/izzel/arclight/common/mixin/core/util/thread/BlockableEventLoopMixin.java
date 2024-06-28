package io.izzel.arclight.common.mixin.core.util.thread;

import io.izzel.arclight.common.mod.server.RunnableInPlace;
import net.minecraft.util.thread.BlockableEventLoop;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockableEventLoop.class)
public class BlockableEventLoopMixin {

    @Inject(method = "execute", cancellable = true, at = @At("HEAD"))
    private void arclight$runInPlace(Runnable runnable, CallbackInfo ci) {
        if (runnable instanceof RunnableInPlace) {
            runnable.run();
            ci.cancel();
        }
    }
}
