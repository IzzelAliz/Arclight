package io.izzel.arclight.common.mixin.core.entity.monster;

import io.izzel.arclight.common.mixin.core.entity.CreatureEntityMixin;
import net.minecraft.entity.monster.ShulkerEntity;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.world.server.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ShulkerEntity.class)
public abstract class ShulkerEntityMixin extends CreatureEntityMixin {

    @Inject(method = "notifyDataManagerChange", at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/entity/monster/ShulkerEntity;forceSetPosition(DDD)V"))
    private void arclight$chunkCheck(DataParameter<?> key, CallbackInfo ci) {
        if (valid) ((ServerWorld) this.world).chunkCheck((ShulkerEntity) (Object) this);
    }
}
