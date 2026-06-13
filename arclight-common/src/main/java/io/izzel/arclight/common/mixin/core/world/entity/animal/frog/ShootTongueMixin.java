package io.izzel.arclight.common.mixin.core.world.entity.animal.frog;

import io.izzel.arclight.common.bridge.core.entity.EntityBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.entity.animal.frog.ShootTongue;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ShootTongue.class)
public class ShootTongueMixin {

    @Inject(method = "eatEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;remove(Lnet/minecraft/world/entity/Entity$RemovalReason;)V"))
    private void arclight$eatCause(ServerLevel serverLevel, Frog frog, CallbackInfo ci) {
        frog.getTongueTarget().ifPresent(entity -> ((EntityBridge) entity).bridge$pushEntityRemoveCause(EntityRemoveEvent.Cause.DEATH));
    }
}
