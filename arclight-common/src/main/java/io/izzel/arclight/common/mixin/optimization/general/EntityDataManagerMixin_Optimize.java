package io.izzel.arclight.common.mixin.optimization.general;

import io.izzel.arclight.common.mod.util.optimization.NoopReadWriteLock;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.locks.ReadWriteLock;

@Mixin(SynchedEntityData.class)
public class EntityDataManagerMixin_Optimize {

    @Shadow @Final @Mutable private ReadWriteLock lock;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void optimize$dropLock(Entity entityIn, CallbackInfo ci) {
        this.lock = NoopReadWriteLock.instance();
    }
}
