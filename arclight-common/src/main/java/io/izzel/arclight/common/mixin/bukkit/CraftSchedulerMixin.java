package io.izzel.arclight.common.mixin.bukkit;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.bukkit.craftbukkit.v.scheduler.CraftScheduler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.concurrent.ThreadFactory;

@Mixin(value = CraftScheduler.class, remap = false)
public class CraftSchedulerMixin {

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/google/common/util/concurrent/ThreadFactoryBuilder;build()Ljava/util/concurrent/ThreadFactory;"))
    private ThreadFactory arclight$setDaemon(ThreadFactoryBuilder instance) {
        return instance.setDaemon(true).build();
    }
}
