package io.izzel.arclight.common.mixin.core.entity.passive;

import io.izzel.arclight.common.mixin.core.entity.CreatureEntityMixin;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.IronGolemEntity;
import org.bukkit.event.entity.EntityTargetEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(IronGolemEntity.class)
public abstract class IronGolemEntityMixin extends CreatureEntityMixin {

    @Inject(method = "collideWithEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/passive/IronGolemEntity;setAttackTarget(Lnet/minecraft/entity/LivingEntity;)V"))
    private void arclight$targetReason(Entity entityIn, CallbackInfo ci) {
        bridge$pushGoalTargetReason(EntityTargetEvent.TargetReason.COLLISION, true);
    }
}
