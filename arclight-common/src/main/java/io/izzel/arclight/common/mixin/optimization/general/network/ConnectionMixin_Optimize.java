package io.izzel.arclight.common.mixin.optimization.general.network;

import io.netty.util.concurrent.AbstractEventExecutor;
import net.minecraft.network.Connection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Slice;

@Mixin(Connection.class)
public class ConnectionMixin_Optimize {

    @ModifyArg(method = "sendPacket", at = @At(value = "INVOKE", remap = false, target = "Lio/netty/channel/EventLoop;execute(Ljava/lang/Runnable;)V"),
        slice = @Slice(from = @At(value = "INVOKE", remap = false, target = "Lio/netty/channel/EventLoop;inEventLoop()Z")))
    private Runnable arclight$useLazyRunnable(Runnable runnable) {
        return (AbstractEventExecutor.LazyRunnable) runnable::run;
    }
}
