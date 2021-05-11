package io.izzel.arclight.impl.mixin.optimization.general;

import io.izzel.arclight.impl.common.optimization.NoopReadWriteLock;
import net.minecraft.entity.Entity;
import net.minecraft.network.datasync.EntityDataManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.locks.ReadWriteLock;

@Mixin(EntityDataManager.class)
public class EntityDataManagerMixin_Optimize {

    @Shadow @Final @Mutable private ReadWriteLock lock;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void optimize$dropLock(Entity entityIn, CallbackInfo ci) {
        this.lock = NoopReadWriteLock.instance();
    }
}
